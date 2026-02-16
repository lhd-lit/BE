package LDHD.project.domain.chat.web.controller;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.domain.chat.service.AiChatService;
import LDHD.project.domain.chat.web.dto.AiMessageRequest;
import LDHD.project.domain.chat.web.dto.AiMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "AI 채팅 API", description = "AI 채팅방 생성 및 조회 API")
@Slf4j
@Controller
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // WebSocket AI 메시지 전송
    @MessageMapping("/ai-chat/send")
    public void sendMessage(@Payload @Valid AiMessageRequest request, Principal principal) {
        try{
            // 1. 사용자 Id 추출 -> Principal: WebSocket 연결 시 JWT 검증 후 설정됨
            Long userId = extractUserId(principal);
            log.debug("AI 채팅 메시지 수신 완료 - userId: {}, chatRoomId: {}, lectureId: {}",
                    userId, request.getChatRoomId(), request.getSelfStudyId());

            // 2. 사용자 메시지 저장
            AiMessageResponse userMessage = aiChatService.saveUserMessage(userId, request);

            // 3. 사용자 메시지 브로드캐스팅
            String destination = "/sub/ai-chat/" + request.getChatRoomId();
            messagingTemplate.convertAndSend(destination, userMessage);

            log.info("사용자 메시지 브로드캐스팅 완료 - messageId: {}, chatRoomId: {}",
                    userMessage.getMessageId(), request.getChatRoomId());

            // 4. AI 응답 생성 트리거 -> Service에서 브로드캐스팅
            aiChatService.processAiResponse(
                    request.getChatRoomId(),
                    request.getSelfStudyId(),
                    request.getContent()
            );
            log.debug("AI 응답 프로세스 실행 - chatRoomId: {}, selfStudyId: {}",
                    request.getChatRoomId(), request.getSelfStudyId());

        }catch(GeneralException e){
            // 비즈니스 예외(채팅방X, 권한X)
            log.warn("AI 채팅 비즈니스 오류 - userId: {}, error: {}",
                    principal.getName(), e.getErrorCode().getMessage());

            sendErrorToUser(principal.getName(), e.getMessage());

        } catch (Exception e) {
            // 시스템 예외 (예상치 못한 에러)
            log.error("AI 채팅 시스템 오류 - userId: {}", principal.getName(), e);

            sendErrorToUser(principal.getName(), "메시지 전송 중 오류가 발생했습니다.");
        }
    }
    // ai 채팅방 생성
    @Operation(summary = "AI 채팅방 생성", description = "사용자 전용 AI 채팅방을 생성합니다.")
    @PostMapping("/api/ai-chat/rooms")
    @ResponseBody
    public Long createChatRoom(Principal principal) {
        Long userId = extractUserId(principal);

        return aiChatService.createChatRoom(userId);
    }

    // 사용자 ai 채팅방 목록 조회
    @Operation(summary = "사용자 AI 채팅방 목록 조회", description = "사용자가 참여 중인 AI 채팅방 ID 목록을 반환합니다.")
    @GetMapping("/api/ai-chat/rooms")
    @ResponseBody
    public List<Long> getUserChatRooms(Principal principal) {
        Long userId = extractUserId(principal);
        return aiChatService.getUserChatRooms(userId); // 채팅방 Id 리스트 반환
    }

    // Ai 메시지 이력 조회(커서 페이징)
    //GET /api/ai-chat/{chatRoomId}/messages?size=20
    //GET /api/ai-chat/{chatRoomId}/messages?cursor=2026-02-16T10:00:00&size=20
    @Operation(summary = "AI 채팅 이력 조회 (커서 페이징)", description = "커서 기반으로 이전 메시지를 조회합니다.")
    @GetMapping("/api/ai-chat/{chatRoomId}/messages")
    @ResponseBody
    public List<AiMessageResponse> getMessages(@PathVariable Long chatRoomId,@RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                   LocalDateTime cursor, // 이전 페이지의 마지막 메시지 시간
                                               @RequestParam(defaultValue = "20") int size) {
        // 페이지 크기 제한 (서버 부하 방지)
        if (size > 100) {
            size = 100;
        }
        return aiChatService.getMessageHistory(chatRoomId, cursor, size);
    }

    // 전체 메시지 조회
    @Operation(summary = "AI 채팅 전체 조회", description = "해당 채팅방의 모든 메시지를 조회합니다.")
    @GetMapping("/api/ai-chat/{chatRoomId}/all-messages")
    @ResponseBody
    public List<AiMessageResponse> getAllMessages(@PathVariable Long chatRoomId) {
        return aiChatService.getAllMessages(chatRoomId);
    }

    // Principal에서 userId 추출
    private Long extractUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("Principal is null or has no name");
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("유효하지 않은 사용자 ID 형식 - name: {}", principal.getName());
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    // 개인 에러 메시지 전송
    private void sendErrorToUser(String username, String message) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                message
        );
    }
}
