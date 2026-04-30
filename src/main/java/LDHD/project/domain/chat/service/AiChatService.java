package LDHD.project.domain.chat.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.chat.client.AiClient;
import LDHD.project.domain.chat.AiMessageRole;
import LDHD.project.domain.chat.entity.AiChatMessage;
import LDHD.project.domain.chat.entity.AiChatRoom;
import LDHD.project.domain.chat.repository.AiChatMessageRepository;
import LDHD.project.domain.chat.repository.AiChatRoomRepository;
import LDHD.project.domain.chat.web.dto.AiMessageRequest;
import LDHD.project.domain.chat.web.dto.AiMessageResponse;
import LDHD.project.domain.chat.web.dto.AiStreamResponse;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiChatService {

    private final AiChatRoomRepository roomRepository;
    private final AiChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SelfStudyRepository selfStudyRepository;
    private final AiClient aiClient;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. 사용자 메시지 저장
    @Transactional
    public AiMessageResponse saveUserMessage(Long userId, AiMessageRequest request) {

        AiChatRoom chatRoom = getRoomWithOwnerValidation(request.getChatRoomId(), userId);

        AiChatMessage userMessage = AiChatMessage.builder()
                .chatRoom(chatRoom)
                .role(AiMessageRole.USER)
                .content(request.getContent())
                .build();

        messageRepository.save(userMessage);

        log.info("사용자 메시지 저장 - messageId: {}, chatRoomId: {}",
                userMessage.getId(), request.getChatRoomId());

        return AiMessageResponse.from(userMessage, userId);
    }

    // 2. AI 스트리밍 응답 처리
    @Async("aiExecutor")
    public void processAiResponse(Long userId, Long chatRoomId, Long selfStudyId, String userPrompt) {

        try {
            // 1. 채팅방 검증
            AiChatRoom chatRoom = getRoomWithOwnerValidation(chatRoomId, userId);

            // 2. 학습 자료 조회
            SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId)
                    .orElseThrow(() -> new GeneralException(ErrorCode.AI_CONTEXT_NOT_FOUND));

            // namespace 반드시 존재해야 함
            String namespace = selfStudy.getNamespace();
            if (namespace == null || namespace.isBlank()) {
                throw new GeneralException(ErrorCode.AI_CONTEXT_NOT_FOUND);
            }

            String sessionId = userId + "_" + chatRoomId;

            StringBuilder fullResponse = new StringBuilder();

            // 스트리밍 처리
            Flux<String> stream = aiClient.streamResponse(sessionId, namespace, userPrompt);

            stream
                    // 시작 이벤트
                    .doOnSubscribe(sub ->
                            sendStreamEvent(chatRoomId, "START", "", null)
                    )

                    // 청크 수신
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        sendStreamEvent(chatRoomId, "CHUNK", chunk, null);
                    })

                    // 완료
                    .doOnComplete(() -> {

                        AiChatMessage aiMessage = AiChatMessage.builder()
                                .chatRoom(chatRoom)
                                .role(AiMessageRole.AI)
                                .content(fullResponse.toString())
                                .build();

                        messageRepository.save(aiMessage);

                        sendStreamEvent(
                                chatRoomId,
                                "END",
                                fullResponse.toString(),
                                aiMessage.getId()
                        );

                        log.info("AI 응답 완료 - chatRoomId: {}", chatRoomId);
                    })

                    // 에러 처리
                    .doOnError(e -> {
                        log.error("AI 스트리밍 오류", e);
                        sendStreamEvent(chatRoomId, "ERROR", "AI 응답 오류", null);
                    })

                    .subscribe();

        } catch (Exception e) {
            log.error("AI 처리 실패", e);
            sendStreamEvent(chatRoomId, "ERROR", "서버 오류", null);
        }
    }

    // 3. 채팅방 생성
    @Transactional
    public Long createChatRoom(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        AiChatRoom chatRoom = AiChatRoom.builder()
                .user(user)
                .build();

        roomRepository.save(chatRoom);

        return chatRoom.getId();
    }

    // 4. 채팅방 목록 조회
    public List<Long> getUserChatRooms(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new GeneralException(ErrorCode.USER_NOT_FOUND);
        }

        return roomRepository.findByUser_Id(userId).stream()
                .map(AiChatRoom::getId)
                .collect(Collectors.toList());
    }


    // 5. 메시지 이력 조회
    public List<AiMessageResponse> getMessageHistory(Long chatRoomId, Long userId, LocalDateTime cursor, int size) {

        getRoomWithOwnerValidation(chatRoomId, userId);

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Slice<AiChatMessage> slice = (cursor == null)
                ? messageRepository.findByChatRoom_IdOrderByCreatedAtDesc(chatRoomId, pageable)
                : messageRepository.findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(chatRoomId, cursor, pageable);

        List<AiChatMessage> messages = new ArrayList<>(slice.getContent());
        Collections.reverse(messages);

        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    // 7. 채팅방 삭제 (세션 초기화 추가)
    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        AiChatRoom chatRoom = getRoomWithOwnerValidation(chatRoomId, userId);

        // AI 서버 세션 초기화 (대화 기록 삭제)
        String sessionId = userId + "_" + chatRoomId;
        try {
            aiClient.clearSession(sessionId);
            log.info("AI 세션 초기화 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("AI 세션 초기화 실패 (무시) - sessionId: {}", sessionId);
        }

        // DB에서 채팅방 삭제 (메시지는 cascade로 삭제되도록 설정 필요)
        roomRepository.delete(chatRoom);
        log.info("채팅방 삭제 완료 - chatRoomId: {}", chatRoomId);
    }

    // 6. 유틸 메서드

    private AiChatRoom getRoomWithOwnerValidation(Long chatRoomId, Long userId) {

        AiChatRoom chatRoom = roomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

        if (!chatRoom.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorCode.FORBIDDEN_USER);
        }

        return chatRoom;
    }

    private AiMessageResponse convertToDto(AiChatMessage message) {

        Long senderId = (message.getRole() == AiMessageRole.USER)
                ? message.getChatRoom().getUser().getId()
                : null;

        return AiMessageResponse.from(message, senderId);
    }

    private void sendStreamEvent(Long chatRoomId, String type, String content, Long messageId) {

        AiStreamResponse response = AiStreamResponse.builder()
                .type(type)
                .chatRoomId(chatRoomId)
                .role(AiMessageRole.AI)
                .content(content)
                .messageId(messageId)
                .build();

        messagingTemplate.convertAndSend("/sub/ai-chat/" + chatRoomId, response);
    }

    // 7. 채팅방 전체 메시지 조회 (추가된 부분)
    public List<AiMessageResponse> getAllMessages(Long chatRoomId, Long userId) {

        // 1. 권한 검증 (내 채팅방이 맞는지)
        getRoomWithOwnerValidation(chatRoomId, userId);

        // 2. 전체 메시지 조회 (과거 -> 최신 순)
        List<AiChatMessage> messages = messageRepository.findByChatRoom_IdOrderByCreatedAtAsc(chatRoomId);

        // 3. DTO로 변환해서 반환
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}