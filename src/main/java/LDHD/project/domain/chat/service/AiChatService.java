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
    private final SimpMessagingTemplate messagingTemplate;
    private final AiClient aiClient;
    private final SelfStudyRepository  selfStudyRepository;

    // 사용자가 보낸 메시지 저장(동기)
    @Transactional
    public AiMessageResponse saveUserMessage(Long userId, AiMessageRequest request) {

        // 1. 채팅방 조회
        AiChatRoom chatRoom = roomRepository.findById(request.getChatRoomId()).orElseThrow(
                ()-> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

        // 2. 권한 확인(본인 채팅방인지)
        if(!chatRoom.getUser().getId().equals(userId)){
            log.warn("허가되지 않은 Ai Chat 접근 - userId: {}, chatRoomId: {}", userId, request.getChatRoomId());
            throw new GeneralException(ErrorCode.FORBIDDEN_USER);
        }

        // 3. 사용자 메시지 생성
        AiChatMessage userMessage = AiChatMessage.builder()
                .chatRoom(chatRoom)
                .role(AiMessageRole.USER)
                .content(request.getContent())
                .build();

        messageRepository.save(userMessage);

        log.info("사용자 메시지 저장 완료 - messageId: {}, chatRoomId: {}", userMessage.getId(), request.getChatRoomId());

        return AiMessageResponse.from(userMessage, userId);
    }

    // ai 응답 생성 및 전송(비동기 - 1. AI API 호출 평균 3~10초)
    @Async("aiExecutor")
    public void processAiResponse(Long chatRoomId, Long selfStudyId, String userPrompt){
        log.debug("Ai Processing started - chatRoomId: {}, selfStudyId: {}", chatRoomId, selfStudyId);

        try{
            // 1. 학습 자료 조회
            SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId).orElseThrow(
                    ()-> new GeneralException(ErrorCode.AI_CONTEXT_NOT_FOUND));

            log.debug("학습 자료 조회 완료 - selfStudyId: {}, title: {}", selfStudyId,  selfStudy.getTitle());

            // 2. 채팅 히스토리 조회
            String chatHistory = buildChatHistory(chatRoomId);

            // 3. ai 응답 생성(AI Client 호출)
            String aiContent = aiClient.generateResponseWithContext(
                    userPrompt,                      // 질문
                    selfStudy.getExtractedText(),      // 문서 컨텍스트
                    chatHistory                      // 대화 히스토리
            );

            log.debug("AI 응답 생성 완료 - chatRoomId: {}, length: {}", chatRoomId, aiContent.length());

            // 4. 채팅방 재조회 (비동기 스레드에서 새 트랜잭션)
            AiChatRoom chatRoom = roomRepository.findById(chatRoomId)
                    .orElseThrow(() -> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

            // 5. AI 메시지 저장 (Enum 사용)
            AiChatMessage aiMessage = AiChatMessage.builder()
                    .chatRoom(chatRoom)
                    .role(AiMessageRole.AI)
                    .content(aiContent)
                    .build();

            messageRepository.save(aiMessage);

            log.info("AI 메시지 저장 완료 - messageId: {}, chatRoomId: {}",
                    aiMessage.getId(), chatRoom.getId());

            // 4. WebSocket 전송 (Push)
            AiMessageResponse response = AiMessageResponse.from(aiMessage, null);
            messagingTemplate.convertAndSend("/sub/ai-chat/"+ chatRoomId, response);

            log.info("AI 응답 전송 완료 - chatRoomId: {}", chatRoomId);

        } catch (Exception e) {
            log.error("AI 생성 실패 - chatRoomId: {}", chatRoomId, e);

            sendErrorResponse(chatRoomId, "AI 응답 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
    // 채팅 히스토리 구성
    private String buildChatHistory(Long chatRoomId) {
        // 1. 최근 10개 메시지 조회 (최신순)
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AiChatMessage> recentMessages = messageRepository
                .findByChatRoom_IdOrderByCreatedAtDesc(chatRoomId, pageable)
                .getContent();

        // 2. 메시지가 없으면 빈 문자열 반환
        if (recentMessages.isEmpty()) {
            return "";
        }
        // 3. 역순 정렬 (시간순으로 변경)
        Collections.reverse(recentMessages);

        // 4. "ROLE: content" 형식으로 변환
        return recentMessages.stream()
                .map(m -> m.getRole().name() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }
    // 에러 응답 전송
    private void sendErrorResponse(Long chatRoomId, String errorMessage) {
        AiMessageResponse errorResponse = AiMessageResponse.builder()
                .messageId(null)
                .chatRoomId(chatRoomId)
                .role(AiMessageRole.AI)
                .senderId(null)
                .content(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/sub/ai-chat/" + chatRoomId, errorResponse );
    }

    // AI 채팅 메시지 이력 조회(커서 페이징)
    public List<AiMessageResponse> getMessageHistory(Long chatRoomId, LocalDateTime cursor, int size){

            Pageable pageable = PageRequest.of(
                    0, size, Sort.by(Sort.Direction.DESC, "createdAt")
            );
            Slice<AiChatMessage> slice;

            if (cursor == null) {
                slice = messageRepository.findByChatRoom_IdOrderByCreatedAtDesc(chatRoomId, pageable);
            } else {
                slice = messageRepository.findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(chatRoomId, cursor, pageable);
            }
            return slice.getContent()
                    .stream()
                    .map(m -> {
                        // role이 USER면 userId 포함, AI면 null
                        Long senderId = m.getRole() == AiMessageRole.USER
                                ? m.getChatRoom().getUser().getId()
                                : null;
                        return AiMessageResponse.from(m, senderId);
                    })
                    .collect(Collectors.toList());

    }
    // 전체 메시지 조회
    public List<AiMessageResponse> getAllMessages(Long chatRoomId) {
        List<AiChatMessage> messages = messageRepository.findByChatRoom_IdOrderByIdAsc(chatRoomId);

        return messages.stream()
                .map(m -> {
                    Long senderId = m.getRole() == AiMessageRole.USER
                            ? m.getChatRoom().getUser().getId()
                            : null;
                    return AiMessageResponse.from(m, senderId);
                })
                .collect(Collectors.toList());
    }
    // AI 채팅방 생성
    @Transactional
    public Long createChatRoom(Long userId){

        // 1. 시용자 존재 확인
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 2. AI 채팅방 생성
        AiChatRoom chatRoom = AiChatRoom.builder()
                .user(user)
                .build();

        roomRepository.save(chatRoom);

        log.info("AI 채팅방 생성 완료 - userId: {}, chatRoomId: {}", userId, chatRoom.getId());
        return chatRoom.getId();
    }

    // 사용자의 AI 채팅방 목록 조히
    public List<Long> getUserChatRooms(Long userId) {
        return roomRepository.findByUser_Id(userId)
                .stream()
                .map(AiChatRoom::getId)
                .collect(Collectors.toList());
    }
}
