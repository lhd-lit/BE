package LDHD.project.domain.chat.web.controller;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.chat.service.AiChatService;
import LDHD.project.domain.chat.web.dto.AiMessageRequest;
import LDHD.project.domain.chat.web.dto.AiMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "AI 채팅 API", description = "AI 채팅방 생성 및 조회 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-chat")
public class AiChatController {

    private final AiChatService aiChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // WebSocket AI 메시지 전송
    @MessageMapping("/send")
    public void sendMessage(@Payload @Valid AiMessageRequest request, Principal principal) {

        String principalName = extractPrincipalName(principal);

        try {
            Long userId = parseUserId(principalName);
            log.debug("AI 채팅 메시지 수신 - userId: {}, chatRoomId: {}, selfStudyId: {}",
                    userId, request.getChatRoomId(), request.getSelfStudyId());

            // 2. 사용자 메시지 저장
            AiMessageResponse userMessage = aiChatService.saveUserMessage(userId, request);

            // 3. 사용자 메시지 브로드캐스팅
            messagingTemplate.convertAndSend("/sub/ai-chat/" + request.getChatRoomId(), userMessage);

            log.info("사용자 메시지 브로드캐스팅 완료 - messageId: {}, chatRoomId: {}",
                    userMessage.getMessageId(), request.getChatRoomId());

            // 4. AI 응답 생성 트리거 -> Service에서 브로드캐스팅
            aiChatService.processAiResponse(
                    userId,
                    request.getChatRoomId(),
                    request.getSelfStudyId(),
                    request.getContent()
            );
            log.debug("AI 응답 프로세스 실행 - chatRoomId: {}, selfStudyId: {}",
                    request.getChatRoomId(), request.getSelfStudyId());

        } catch (GeneralException e) {
            log.warn("AI 채팅 비즈니스 오류 - principalName: {}, error: {}",
                    principalName, e.getErrorCode().getMessage());
            sendErrorToUser(principalName, e.getMessage());

        } catch (IllegalArgumentException e) {
            // extractUserId 실패 등 인증 파싱 오류
            log.error("AI 채팅 인증 오류 - principalName: {}, error: {}", principalName, e.getMessage());
            sendErrorToUser(principalName, "인증 정보가 유효하지 않습니다.");

        } catch (Exception e) {
            log.error("AI 채팅 시스템 오류 - principalName: {}", principalName, e);
            sendErrorToUser(principalName, "메시지 전송 중 오류가 발생했습니다.");
        }

    }
    // ai 채팅방 생성
    @Operation(summary = "AI 채팅방 생성", description = "사용자 전용 AI 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<GlobalResponse> createChatRoom(@Parameter(name = "X-USER-ID",required = true, in = ParameterIn.HEADER)
                                                          @RequestHeader("X-USER-ID") Long currentUserId
    ) {
        Long chatRoomId = aiChatService.createChatRoom(currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, chatRoomId);
    }

    // 사용자 ai 채팅방 목록 조회
    @Operation(summary = "사용자 AI 채팅방 목록 조회", description = "사용자가 참여 중인 AI 채팅방 ID 목록을 반환합니다.")
    @GetMapping("/rooms")
    public ResponseEntity<GlobalResponse> getUserChatRooms(@Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                            @RequestHeader("X-USER-ID") Long currentUserId
    ) {
        List<Long> rooms = aiChatService.getUserChatRooms(currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, rooms);
    }

    // Ai 메시지 이력 조회(커서 페이징)
    //GET /api/ai-chat/{chatRoomId}/messages?size=20
    //GET /api/ai-chat/{chatRoomId}/messages?cursor=2026-02-16T10:00:00&size=20
    @Operation(summary = "AI 채팅 이력 조회 (커서 페이징)", description = "커서 기반으로 이전 메시지를 조회합니다.")
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<GlobalResponse> getMessages(@PathVariable Long chatRoomId,@Parameter(name = "X-USER-ID",
                                                     required = true, in = ParameterIn.HEADER)
                                                     @RequestHeader("X-USER-ID") Long currentUserId, @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
                                                     @RequestParam(defaultValue = "20") int size)
    {
        int safeSize = Math.min(size, 100);

        List<AiMessageResponse> response = aiChatService.getMessageHistory(chatRoomId, currentUserId, cursor, safeSize);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 전체 메시지 조회
    @Operation(summary = "AI 채팅 전체 조회", description = "해당 채팅방의 모든 메시지를 조회합니다.")
    @GetMapping("/{chatRoomId}/all-messages")
    public ResponseEntity<GlobalResponse> getAllMessages(@PathVariable Long chatRoomId,
                                                         @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                         @RequestHeader("X-USER-ID") Long currentUserId
    ) {
        List<AiMessageResponse> response = aiChatService.getAllMessages(chatRoomId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // Principal에서 userId 추출
    private String extractPrincipalName(Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("Principal이 null이거나 name이 없습니다.");
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return principal.getName();
    }
    // principalName(userId 문자열) → Long 변환
    private Long parseUserId(String principalName) {
        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException e) {
            log.error("유효하지 않은 사용자 ID 형식 - principalName: {}", principalName);
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    // 개인 에러 메시지 전송
    private void sendErrorToUser(String username, String message) {
        if (username == null) {
            log.warn("username이 null이어서 에러 메시지를 전송할 수 없습니다.");
            return;
        }
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", message);
    }
}
