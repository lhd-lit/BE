package LDHD.project.domain.notification.repository;

import LDHD.project.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 커서 페이징 - 전체 알림 (최신순)
    Slice<Notification> findByReceiver_IdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    // 커서 페이징 - cursor 이전 알림
    Slice<Notification> findByReceiver_IdAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long receiverId, LocalDateTime cursor, Pageable pageable);

    // 읽지 않은 알림 개수
    long countByReceiver_IdAndReadFalse(Long receiverId);

    // 특정 알림 목록 일괄 조회 (읽음 처리용)
    @Query("SELECT n FROM Notification n WHERE n.id IN :ids AND n.receiver.id = :receiverId")
    List<Notification> findAllByIdsAndReceiverId(
            @Param("ids") List<Long> ids,
            @Param("receiverId") Long receiverId);

    // 전체 읽음 처리 (Bulk Update - 개별 dirty checking 방지)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.receiver.id = :receiverId AND n.read = false")
    int markAllAsRead(@Param("receiverId") Long receiverId);

    // 선택 읽음 처리 (Bulk Update)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids AND n.receiver.id = :receiverId")
    int markAsReadByIds(@Param("ids") List<Long> ids, @Param("receiverId") Long receiverId);
}
