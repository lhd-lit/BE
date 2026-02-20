package LDHD.project.domain.notification.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NotificationRequest {

    @NotNull(message = "알림 ID 목록은 필수입니다.")
    private List<Long> notificationIds;
}
