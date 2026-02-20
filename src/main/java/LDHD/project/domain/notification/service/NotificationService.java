package LDHD.project.domain.notification.service;

import LDHD.project.domain.notification.NotificationType;
import LDHD.project.domain.notification.entity.Notification;
import LDHD.project.domain.notification.repository.NotificationRepository;
import LDHD.project.domain.notification.web.dto.NotificationResponse;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketSender webSocketSender;

    // 알림 생성 + WebSocket 실시간 전송 + 인원 많을 때 대비 => 비동기 처리 필요
    @Transactional
    public void sendNotifications(List<Long> receiverIds, NotificationType type,
                                       String message, Long targetId){

        // Id가 null, 비어있는 경우 불필요한 DB 접근 방지
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }

        // 1. 수신자 목록 일괄 조회 (N+1 방지)
        List<User> receivers = userRepository.findAllByIdIn(receiverIds);

        if (receivers.isEmpty()) { // 수신자 목록이 비어 있을 경우
            log.warn("알림 수신자를 찾을 수 없음 - receiverIds: {}", receiverIds);
            return;
        }

        // 2. 알림 엔티티 일괄 생성
        List<Notification> notifications = receivers.stream()
                .map(receiver -> Notification.create(receiver, type, message, targetId))
                .collect(Collectors.toList());

        // 3. 일괄 저장 (saveAll - 단건 save 반복 방지)
        notificationRepository.saveAll(notifications);
        log.info("알림 저장 완료 - type: {}, 대상: {}명", type, notifications.size());

        // 4. 각 수신자에게 WebSocket 실시간 전송
        // DB 저장 후 실시간 전송
        notifications.forEach(notification ->
                webSocketSender.send(
                        notification.getReceiver().getId(),
                        NotificationResponse.from(notification)
                )
        );

    }

    // 알림 목록 조회
    public List<NotificationResponse> getNotifications(Long userId, LocalDateTime cursor, int size) {
        // 조회용 Pageable 객체 생성 (최신순 정렬)
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Notification> slice;

        // 커서 유무에 따라 첫 페이지 조회인지, 다음 페이지 조회인지
        if (cursor == null) {
            slice = notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(userId, pageable);
        } else {
            slice = notificationRepository.findByReceiver_IdAndCreatedAtBeforeOrderByCreatedAtDesc(userId, cursor, pageable);
        }

        // 최신순(DESC)으로 가져온 데이터를 클라이언트 UI 상에 맞게 과거->최신순으로 뒤집음
        List<Notification> notifications = new ArrayList<>(slice.getContent());
        Collections.reverse(notifications);

        return notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }
    // 읽지 않은 알림 개수 조회
    public long getUnreadCount(Long userId) {

        return notificationRepository.countByReceiver_IdAndReadFalse(userId);
    }

    // 선택적 읽음 처리
    @Transactional
    public void markAsRead(Long userId, List<Long> notificationIds) {

        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        // 타인 알림 조작 방지
        int updated = notificationRepository.markAsReadByIds(notificationIds, userId);
        log.info("알림 읽음 처리 완료 - userId: {}, 처리 건수: {}", userId, updated);
    }

    // 전체 읽음 처리
    @Transactional
    public void markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(userId);
        log.info("전체 알림 읽음 처리 완료 - userId: {}, 처리 건수: {}", userId, updated);
    }

}
