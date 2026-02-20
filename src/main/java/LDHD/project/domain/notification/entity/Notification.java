package LDHD.project.domain.notification.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.notification.NotificationType;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;


@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 수신자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    // 알림 본문 메시지
    @Column(nullable = false, length = 250)
    private String message;

    // 알림 클릭 시 이동할 대상 (채팅방 ID 등)
    @Column(name = "target_id")
    private Long targetId;

    // 읽음 여부
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Builder
    public Notification(Long id, User receiver, NotificationType type, String message, Long targetId, boolean isRead) {
        this.id = id;
        this.receiver = receiver;
        this.type = type;
        this.message = message;
        this.targetId = targetId;
        this.read = isRead;
    }

    public static Notification create(User receiver, NotificationType type,
                                      String message, Long targetId) {
        return Notification.builder()
                .receiver(receiver)
                .type(type)
                .message(message)
                .targetId(targetId)
                .build();
    }

    // 알림 읽음 처리
    public void markAsRead() {
        if (!this.read) {
            this.read = true;
        }
    }
}
