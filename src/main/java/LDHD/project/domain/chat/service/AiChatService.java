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
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final SimpMessagingTemplate messagingTemplate; // Controller와 동일한 전송 도구 사용

    // 사용자 메시지 저장
    @Transactional
    public AiMessageResponse saveUserMessage(Long userId, AiMessageRequest request) {
        // 1. 방 조회 및 소유권 검증 (DB 1회)
        AiChatRoom chatRoom = getRoomWithOwnerValidation(request.getChatRoomId(), userId);

        // 2. 메시지 엔티티 생성
        AiChatMessage userMessage = AiChatMessage.builder()
                .chatRoom(chatRoom)
                .role(AiMessageRole.USER)
                .content(request.getContent())
                .build();

        // 3. 저장
        messageRepository.save(userMessage);
        log.info("사용자 메시지 저장 완료 - messageId: {}, chatRoomId: {}", userMessage.getId(), request.getChatRoomId());

        // 4. DTO 반환 (Controller가 이를 받아 즉시 브로드캐스팅함)
        return AiMessageResponse.from(userMessage, userId);
    }

    @Async("aiExecutor")
    @Transactional
    public void processAiResponse(Long userId, Long chatRoomId, Long selfStudyId, String userPrompt) {
        log.debug("AI 응답 생성 프로세스 시작 - chatRoomId: {}", chatRoomId);

        try {
            // 권한 검증
            AiChatRoom chatRoom = getRoomWithOwnerValidation(chatRoomId, userId);

            // 1. 학습 자료(Context) 조회
            SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId)
                    .orElseThrow(() -> new GeneralException(ErrorCode.AI_CONTEXT_NOT_FOUND));

            // 2. 이전 대화 기록 구성 (최근 10개)
            String chatHistory = buildChatHistory(chatRoomId);

            // 3. AI 모델 호출 (외부 API - 시간 소요)
            String aiContent = aiClient.generateResponseWithContext(
                    userPrompt,
                    selfStudy.getExtractedText(),
                    chatHistory
            );

            // 5. AI 메시지 저장
            AiChatMessage aiMessage = AiChatMessage.builder()
                    .chatRoom(chatRoom)
                    .role(AiMessageRole.AI)
                    .content(aiContent)
                    .build();

            messageRepository.save(aiMessage);
            log.info("AI 메시지 저장 완료 - messageId: {}", aiMessage.getId());

            // 6. WebSocket 브로드캐스팅 (AI 메시지는 Service에서 직접 전송)
            // AI 메시지의 senderId는 null로 처리
            AiMessageResponse response = AiMessageResponse.from(aiMessage, null);
            messagingTemplate.convertAndSend("/sub/ai-chat/" + chatRoomId, response);

            log.info("AI 응답 전송 완료 - chatRoomId: {}", chatRoomId);

        } catch (Exception e) {
            log.error("AI 응답 생성 실패 - chatRoomId: {}", chatRoomId, e);

            sendErrorResponse(chatRoomId, "죄송합니다. AI 응답을 생성하는 중 문제가 발생했습니다.");
        }
    }

    // AI 채팅방 생성
    @Transactional
    public Long createChatRoom(Long userId) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        AiChatRoom chatRoom = AiChatRoom.builder()
                .user(user)
                .build();

        roomRepository.save(chatRoom);
        log.info("AI 채팅방 생성 완료 - userId: {}, chatRoomId: {}", userId, chatRoom.getId());

        return chatRoom.getId();
    }

    // 내 채팅방 목록 조회
    public List<Long> getUserChatRooms(Long userId) {
        return roomRepository.findByUser_Id(userId).stream()
                .map(AiChatRoom::getId)
                .collect(Collectors.toList());
    }

    // 메시지 이력 조회 (커서 페이징)
    public List<AiMessageResponse> getMessageHistory(Long chatRoomId, Long userId, LocalDateTime cursor, int size) {
        // 권한 검증 (단순 조회)
        getRoomWithOwnerValidation(chatRoomId, userId);

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<AiChatMessage> slice;

        if (cursor == null) {
            slice = messageRepository.findByChatRoom_IdOrderByCreatedAtDesc(chatRoomId, pageable);
        } else {
            slice = messageRepository.findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(chatRoomId, cursor, pageable);
        }

        // DESC로 가져온 후 뒤집어서 ASC(과거→최신) 순으로 반환
        List<AiChatMessage> messages = new ArrayList<>(slice.getContent());
        Collections.reverse(messages);

        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 전체 메시지 조회
    public List<AiMessageResponse> getAllMessages(Long chatRoomId, Long userId) {
        getRoomWithOwnerValidation(chatRoomId, userId);

        return messageRepository.findByChatRoom_IdOrderByIdAsc(chatRoomId).stream()
                .map(this::convertToDto) // 메서드 참조로 중복 제거
                .collect(Collectors.toList());
    }

    // 방 조회 + 소유자 검증 통합 (검증된 방 객체 반환)
    private AiChatRoom getRoomWithOwnerValidation(Long chatRoomId, Long userId) {
        AiChatRoom chatRoom = roomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

        if (!chatRoom.getUser().getId().equals(userId)) {
            log.warn("권한 없는 사용자 접근 - userId: {}, chatRoomId: {}", userId, chatRoomId);
            throw new GeneralException(ErrorCode.FORBIDDEN_USER);
        }
        return chatRoom;
    }

    // DTO 변환
    private AiMessageResponse convertToDto(AiChatMessage message) {
        Long senderId = (message.getRole() == AiMessageRole.USER)
                ? message.getChatRoom().getUser().getId()
                : null;
        return AiMessageResponse.from(message, senderId);
    }

    // 채팅 기록 문자열 변환 (AI Context용)
    private String buildChatHistory(Long chatRoomId) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<AiChatMessage> recentMessages = messageRepository
                .findByChatRoom_IdOrderByCreatedAtDesc(chatRoomId, pageable)
                .getContent();

        if (recentMessages.isEmpty()) return "";

        // 최신순 조회를 시간순으로 정렬 (AI에게는 과거->현재 순으로 줘야 함)
        List<AiChatMessage> history = new java.util.ArrayList<>(recentMessages);
        Collections.reverse(history);

        return history.stream()
                .map(m -> m.getRole().name() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    // 에러 메시지 전송 (WebSocket)
    private void sendErrorResponse(Long chatRoomId, String errorMessage) {
        AiMessageResponse errorResponse = AiMessageResponse.builder()
                .chatRoomId(chatRoomId)
                .role(AiMessageRole.AI)
                .content(errorMessage)
                .createdAt(LocalDateTime.now())
                .build(); // messageId, senderId는 null

        messagingTemplate.convertAndSend("/sub/ai-chat/" + chatRoomId, errorResponse);
    }
}
