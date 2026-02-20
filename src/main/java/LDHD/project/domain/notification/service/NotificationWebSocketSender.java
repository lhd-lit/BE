package LDHD.project.domain.notification.service;

import LDHD.project.domain.notification.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketSender {

    private final SimpMessagingTemplate messagingTemplate;

    // 단일 사용자에게 알림 전송
    public void send(Long userId, NotificationResponse response) {
        try {
            // STOMP 개인 큐 경로: /user/{userId}/queue/notification
            String destination = "/user/" + userId + "/queue/notification";
            messagingTemplate.convertAndSend(destination, response);
            log.debug("알림 WebSocket 전송 완료 - userId: {}, type: {}", userId, response.getType());
        } catch (Exception e) {
            // WebSocket 전송 실패 → 알림 저장은 이미 완료된 상태
            // 전송 실패해도 사용자는 REST API로 알림 조회 가능하므로 예외 전파 X
            log.error("알림 WebSocket 전송 실패 - userId: {}", userId, e);
        }
    }
    // 다수 사용자에게 일괄 전송
    public void sendAll(List<Long> userIds, NotificationResponse response) {
        userIds.forEach(userId -> send(userId, response));
    }
}
