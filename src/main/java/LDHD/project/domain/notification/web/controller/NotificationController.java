package LDHD.project.domain.notification.web.controller;


import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.domain.notification.service.NotificationService;
import LDHD.project.domain.notification.web.dto.NotificationRequest;
import LDHD.project.domain.notification.web.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "알림 API", description = "알림 조회 및 읽음 처리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 목록 조회
    @Operation(summary = "알림 목록 조회", description = "커서 기반으로 알림을 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse> getNotifications(Principal principal, @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
                                                @RequestParam(defaultValue = "20") int size){

        Long userId = extractUserId(principal);
        int safeSize = Math.min(size, 100);

        List<NotificationResponse> response =
                notificationService.getNotifications(userId, cursor, safeSize);

        return GlobalResponse.onSuccess(SuccessCode.OK, response);
    }

    // 읽지 않은 알림 개수 조회
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "뱃지 표시용 미읽음 알림 수를 반환합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<GlobalResponse> getUnreadCount(Principal principal) {

        Long userId = extractUserId(principal);
        long count = notificationService.getUnreadCount(userId);

        return GlobalResponse.onSuccess(SuccessCode.OK, count);
    }

    // 선택 읽음 처리
    @Operation(summary = "선택 알림 읽음 처리", description = "선택한 알림들을 읽음 처리합니다.")
    @PatchMapping("/read")
    public ResponseEntity<GlobalResponse> markAsRead(Principal principal,@RequestBody @Valid NotificationRequest request) {

        Long userId = extractUserId(principal);

        notificationService.markAsRead(userId, request.getNotificationIds());

        return GlobalResponse.onSuccess(SuccessCode.OK, null);
    }

    // 전체 읽음 처리
    @Operation(summary = "전체 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ResponseEntity<GlobalResponse> markAllAsRead(Principal principal) {

        Long userId = extractUserId(principal);
        notificationService.markAllAsRead(userId);

        return GlobalResponse.onSuccess(SuccessCode.OK, null);
    }

    // Principal에서 userId 추출
    private Long extractUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }
}
