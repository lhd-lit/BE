package LDHD.project.domain.chat.web.controller;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.domain.chat.service.GroupChatService;
import LDHD.project.domain.chat.web.dto.GroupMessageRequest;
import LDHD.project.domain.chat.web.dto.GroupMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "그룹 채팅 API", description = "그룹 채팅 조회 API (WebSocket 전송 제외)")
@Slf4j
@Controller
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // 그룹 내 메시지 전송
    @MessageMapping("/group-chat/send") // principal : 인증된 사용자 정보(userId)
    public void sendMessage(@Payload @Validated GroupMessageRequest request, Principal principal) {
        try{
            // 1. 사용자 ID 추출
            Long userId = extractUserId(principal);
            log.debug("그룹 메시지 수신 완료 - userId: {}, chatRoomId: {}",
                    userId, request.getChatRoomId());

            // 2. DB 저장, 권한 검증(서비스 로직)
            GroupMessageResponse response = groupChatService.sendMessage(userId, request);

            // 3. 해당 채팅방 구독하는 사용자 전체에게 전송(브로드캐스팅)
            String destination = "/sub/group-chat/" + request.getChatRoomId();
            messagingTemplate.convertAndSend(destination, response);

            log.info("그룹 메시지 브로드캐스팅 완료 - messageId: {}, chatRoomId: {}",
            response.getMessageId(), request.getChatRoomId());

        }catch (GeneralException e){
            // 예외 처리
            log.warn("그룹 채팅 비즈니스 로직 오류 - userId: {}, error: {}",
                    principal.getName(), e.getErrorCode().getMessage());

            sendErrorToUser(principal.getName(), e.getMessage());

        }catch (Exception e) {
            // 시스템 예외 처리
            log.error("그룹 채팅 시스템 오류 - userId: {}", principal.getName(), e);

            sendErrorToUser(principal.getName(), "메시지 전송 중 오류가 발생했습니다.");
        }
    }

    // 그룹 메시지 이력 조회
    @Operation(summary = "그룹 채팅 이력 조회 (커서 페이징)", description = "특정 시점(cursor) 이전의 메시지를 size만큼 조회합니다.")
    @GetMapping("/api/group-chat/{chatRoomId}/messages")
    @ResponseBody
    public List<GroupMessageResponse> getMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        // 페이지 크기 제한
        if (size > 100) {
            size = 100;
        }

        return groupChatService.getMessageHistory(chatRoomId, cursor, size);
    }

    // 전체 메시지 조회
    @Operation(summary = "그룹 채팅 전체 조회", description = "채팅방의 모든 메시지를 조회합니다 (개발용).")
    @GetMapping("/api/group-chat/{chatRoomId}/all-messages")
    @ResponseBody
    public List<GroupMessageResponse> getAllMessages(@PathVariable Long chatRoomId) {

        return groupChatService.getAllMessages(chatRoomId);
    }

    // 읽지 않은 메시지 수 조회
    @Operation(summary = "읽지 않은 메시지 수 조회", description = "사용자가 마지막으로 읽은 시간 이후 쌓인 메시지 개수를 반환합니다.")
    @GetMapping("/api/group-chat/{chatRoomId}/unread-count")
    @ResponseBody
    public Long getUnreadCount(@PathVariable Long chatRoomId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                   LocalDateTime lastReadTime // 마지막으로 읽은 시간
    ) {
        return groupChatService.getUnreadMessageCount(chatRoomId, lastReadTime);
    }

    // Principal - userId 추출 (권한 검증 위함 - sendMessage)
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

    // 개인에게 에러 메시지 전송
    private void sendErrorToUser(String username, String message) {
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/errors",
                message
        );
    }
}
