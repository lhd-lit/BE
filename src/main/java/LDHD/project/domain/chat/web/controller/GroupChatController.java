package LDHD.project.domain.chat.web.controller;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.chat.service.GroupChatService;
import LDHD.project.domain.chat.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "그룹 채팅 API", description = "그룹 채팅 조회 API (WebSocket 전송 제외)")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group-chat")
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final SimpMessagingTemplate messagingTemplate;

    // 그룹 내 메시지 전송
    @MessageMapping("/group-chat/send") // principal : 인증된 사용자 정보(userId)
    public void sendMessage(@Payload @Validated GroupMessageRequest request, Principal principal) {

        String principalName = extractPrincipalName(principal);

        try{
            // 1. 사용자 ID 추출
            Long userId = parseUserId(principalName);

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

        }catch (IllegalArgumentException e) {
            log.error("그룹 채팅 인증 오류 - principalName: {}, error: {}", principalName, e.getMessage());
            sendErrorToUser(principalName, "인증 정보가 유효하지 않습니다.");

        }catch (Exception e) {
            // 시스템 예외 처리
            log.error("그룹 채팅 시스템 오류 - userId: {}", principal.getName(), e);

            sendErrorToUser(principal.getName(), "메시지 전송 중 오류가 발생했습니다.");
        }
    }

    // 그룹 채팅방 생성
    @Operation(summary = "그룹 채팅방 생성", description = "스터디 그룹 전용 채팅방을 생성합니다.")
    @PostMapping("/rooms")
    public ResponseEntity<GlobalResponse> createChatRoom(@RequestBody @Valid CreateGroupChatRequest request,
                                                         @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                         @RequestHeader("X-USER-ID") Long currentUserId) {
        GroupChatRoomResponse response = groupChatService.createChatRoom(request, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.CREATED, response);
    }

    // 그룹 채팅방 목록 조회
    @Operation(summary = "내 그룹 채팅방 목록 조회", description = "사용자가 참여 중인 그룹 채팅방 목록을 페이징으로 반환합니다.")
    @GetMapping("/rooms")
    public ResponseEntity<GlobalResponse> getMyChatRooms(@Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                         @RequestHeader("X-USER-ID") Long currentUserId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {

        // 상한선 설정
        int safeSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, safeSize);

        Page<GroupChatRoomResponse> response = groupChatService.getMyChatRooms(currentUserId, pageable);
        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 그룹 채팅방 삭제
    @Operation(summary = "그룹 채팅방 삭제", description = "방장(LEADER)만 채팅방을 삭제할 수 있습니다.")
    @DeleteMapping("/rooms")
    public ResponseEntity<GlobalResponse> deleteChatRoom(@RequestBody @Valid DeleteGroupChatRequest request,
                                                         @Parameter(name = "X-USER-ID", required = true,
                                                          in = ParameterIn.HEADER)
                                                         @RequestHeader("X-USER-ID") Long currentUserId) {

        groupChatService.deleteChatRoom(request, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, null);
    }

    // 그룹 채팅방에 사용자 초대
    @Operation(summary = "그룹 채팅 사용자 초대", description = "그룹 멤버를 채팅방에 초대합니다.")
    @PostMapping("/{studyGroupId}/invite")
    public ResponseEntity<GlobalResponse> inviteUsers(@PathVariable Long studyGroupId,@RequestBody @Valid InviteGroupChatMemberRequest request,
                                                      @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
                                                      @RequestHeader("X-USER-ID") Long currentUserId) {

        groupChatService.inviteUsers(studyGroupId, request, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, null);
    }


    // 그룹 메시지 이력 조회
    @Operation(summary = "그룹 채팅 이력 조회 (커서 페이징)",description = "특정 시점 이전의 메시지를 size만큼 조회합니다.")
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<GlobalResponse> getMessages(
            @PathVariable Long chatRoomId,
            @Parameter(name = "X-USER-ID", required = true, in = ParameterIn.HEADER)
            @RequestHeader("X-USER-ID") Long currentUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, 100);

        List<GroupMessageResponse> response = groupChatService.getMessageHistory(
                currentUserId, chatRoomId, cursor, safeSize);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 전체 메시지 조회
    @Operation(summary = "그룹 채팅 전체 조회", description = "채팅방의 모든 메시지를 조회합니다 (개발용).")
    @GetMapping("/{chatRoomId}/all-messages")
    public ResponseEntity<GlobalResponse> getAllMessages( @PathVariable Long chatRoomId, @Parameter(name = "X-USER-ID",
                                                          required = true, in = ParameterIn.HEADER)
                                                          @RequestHeader("X-USER-ID") Long currentUserId) {

        List<GroupMessageResponse> response = groupChatService.getAllMessages(chatRoomId, currentUserId);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 읽지 않은 메시지 수 조회
    @Operation(summary = "읽지 않은 메시지 수 조회", description = "사용자가 마지막으로 읽은 시간 이후 쌓인 메시지 개수를 반환합니다.")
    @GetMapping("/{chatRoomId}/unread-count")
    public ResponseEntity<GlobalResponse> getUnreadCount( @PathVariable Long chatRoomId, @Parameter(name = "X-USER-ID",
                                                           required = true, in = ParameterIn.HEADER)
                                                           @RequestHeader("X-USER-ID") Long currentUserId, @RequestParam
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                           LocalDateTime lastReadTime) {

        Long count = groupChatService.getUnreadMessageCount(chatRoomId, currentUserId, lastReadTime);

        return GlobalResponse.onSuccess(SuccessCode.OK, count);
    }

    // Principal - userId 추출 (권한 검증 위함 - sendMessage)
    private String extractPrincipalName(Principal principal) {
        if (principal == null || principal.getName() == null) {
            log.error("Principal이 null이거나 name이 없습니다.");
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return principal.getName();
    }
    // principalName → Long userId 변환
    private Long parseUserId(String principalName) {
        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException e) {
            log.error("유효하지 않은 사용자 ID 형식 - principalName: {}", principalName);
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    // 개인에게 에러 메시지 전송
    private void sendErrorToUser(String username, String message) {
        if (username == null) {
            log.warn("username이 null이어서 에러 메시지를 전송할 수 없습니다.");
            return;
        }
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", message);
    }
}
