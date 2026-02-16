package LDHD.project.common.config;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.security.jwt.JwtTokenProvider;
import LDHD.project.domain.chat.entity.AiChatRoom;
import LDHD.project.domain.chat.entity.GroupChatRoom;
import LDHD.project.domain.chat.repository.AiChatRoomRepository;
import LDHD.project.domain.chat.repository.GroupChatRoomRepository;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupChatRoomRepository groupChatRoomRepository;
    private final AiChatRoomRepository aiChatRoomRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        // 1. 프론트엔드에서 연결할 웹소켓 주소
        // ws://localhost:8080/ws-stomp
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // 모든 도메인 허용 (CORS 해결)
                .withSockJS(); // SockJS 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 메시지 보낼 때 (Publish) 경로: /pub/chat/message
        registry.setApplicationDestinationPrefixes("/pub");

        // 3. 메시지 받을 때 (Subscribe) 경로: /sub/chat/room/{id}
        registry.enableSimpleBroker("/sub");
    }

    // 인바운드 채널 설정
    // SUBSCRIBE(구독) 명령 시 권한 검증 / 권한 X -> 접근 허가 X
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    // 1. WebSocket 연결 시 JWT 인증
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        authenticateWebSocket(accessor);
                    }

                    // 2. 채팅방 구독 시 권한 검증
                    else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        validateSubscription(accessor);
                    }
                }

                return message;
            }
        });
    }

    // WebSocket 연결 시 JWT 인증
    private void authenticateWebSocket(StompHeaderAccessor accessor) {
        try {
            // 1. Authorization 헤더에서 토큰 추출
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("WebSocket 연결 실패: Authorization 헤더 없음");
                throw new GeneralException(ErrorCode.TOKEN_NOT_FOUND);
            }

            // "Bearer " 제거하고 순수 토큰만 추출
            String token = authorization.substring(7);

            // 2. 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket 연결 실패: 유효하지 않은 토큰");
                throw new GeneralException(ErrorCode.INVALID_TOKEN);
            }

            // 3. ACCESS 토큰인지 확인 (REFRESH 토큰으로 WebSocket 연결 방지)
            jwtTokenProvider.validateTokenType(token, "ACCESS");

            // 4. 토큰에서 userId 추출
            long userId = jwtTokenProvider.getUserId(token);

            // 5. Principal 설정 (이후 모든 메시지에서 userId 사용 가능)
            accessor.setUser(() -> String.valueOf(userId));

            log.info("WebSocket 인증 성공 - userId: {}", userId);

        } catch (GeneralException e) {
            log.error("WebSocket 인증 실패: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("WebSocket 인증 중 예외 발생", e);
            throw new GeneralException(ErrorCode.WEBSOCKET_AUTH_FAILED);
        }
    }


    // 구독 권한 검증
    // 1. 그룹 채팅방: /sub/group-chat/{chatRoomId} -> 해당 스터디 그룹의 멤버인지 확인
    // 2. AI 채팅방: /sub/ai-chat/{chatRoomId} -> 본인 채팅방인지 확인(SelfStudy와 더블 체크)
    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        // 사용자 인증 정보 추출
        Long userId = extractUserId(accessor);

        if (userId == null) {
            log.warn("구독 실패: 인증 정보 없음 - destination: {}", destination);
            throw new AccessDeniedException("인증 정보가 없습니다.");
        }

        // 1. 그룹 채팅방 구독 검증
        if (destination.startsWith("/sub/group-chat/")) {
            validateGroupChatSubscription(destination, userId);
        }

        // 2. AI 채팅방 구독 검증
        else if (destination.startsWith("/sub/ai-chat/")) {
            validateAiChatSubscription(destination, userId);
        }

        // 3. 개인 에러 큐 구독 (항상 허용)
        else if (destination.startsWith("/user/queue/errors")) {
            log.debug("에러 큐 구독 - userId: {}", userId);
        }
    }

    // 그룹 채팅방 구독 검증
    private void validateGroupChatSubscription(String destination, Long userId) {
        try {
            // 1. destination에서 chatRoomId 추출
            Long chatRoomId = extractChatRoomId(destination);

            // 2. 채팅방 조회
            GroupChatRoom chatRoom = groupChatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new AccessDeniedException("채팅방을 찾을 수 없습니다."));

            // 3. StudyGroup ID 추출
            Long studyGroupId = chatRoom.getStudyGroup().getId();

            // 4. 멤버십 검증
            if (!groupMemberRepository.existsByStudyGroup_IdAndUser_Id(studyGroupId, userId)) {
                log.warn("그룹 채팅방 구독 거부 - userId: {}, chatRoomId: {}, studyGroupId: {}",
                        userId, chatRoomId, studyGroupId);
                throw new AccessDeniedException("이 채팅방에 접근 권한이 없습니다.");
            }

            log.info("그룹 채팅방 구독 승인 - userId: {}, chatRoomId: {}", userId, chatRoomId);

        } catch (NumberFormatException e) {
            log.error("잘못된 chatRoomId - destination: {}", destination);
            throw new AccessDeniedException("잘못된 채팅방 ID입니다.");
        }
    }

    // ai 채팅방 구독 검증
    private void validateAiChatSubscription(String destination, Long userId) {
        try {
            // 1. chatRoomId 추출
            Long chatRoomId = extractChatRoomId(destination);

            // 2. AI 채팅방 조회
            AiChatRoom room = aiChatRoomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new AccessDeniedException("채팅방을 찾을 수 없습니다."));

            // 3. 본인 채팅방 확인
            if (!room.getUser().getId().equals(userId)) {
                log.warn("AI 채팅방 구독 거부 - userId: {}, chatRoomId: {}, ownerId: {}",
                        userId, chatRoomId, room.getUser().getId());
                throw new AccessDeniedException("본인의 채팅방만 접근 가능합니다.");
            }

            log.info("AI 채팅방 구독 승인 - userId: {}, chatRoomId: {}", userId, chatRoomId);

        } catch (NumberFormatException e) {
            log.error("잘못된 chatRoomId - destination: {}", destination);
            throw new AccessDeniedException("잘못된 채팅방 ID입니다.");
        }
    }
    // destination에서 chatRoomId 추출
    private Long extractChatRoomId(String destination) {

        String[] parts = destination.split("/");

        return Long.parseLong(parts[parts.length - 1]);
    }

    // StompHeaderAccessor에서 userId 추출
    private Long extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            return null;
        }

        try {
            return Long.parseLong(accessor.getUser().getName());
        } catch (NumberFormatException e) {
            log.error("Invalid userId format: {}", accessor.getUser().getName());
            return null;
        }
    }

}
