package LDHD.project.domain.notification.web.dto;

import LDHD.project.domain.notification.NotificationType;
import LDHD.project.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private final Long notificationId;
    private final NotificationType type;
    private final String message;
    private final Long targetId;        // 채팅방 ID 등 클릭 시 이동 대상
    private final boolean read;
    private final LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .type(notification.getType())
                .message(notification.getMessage())
                .targetId(notification.getTargetId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
