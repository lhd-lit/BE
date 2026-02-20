package LDHD.project.domain.notification;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.management.NotificationListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(NotificationListener.class)
@Table(name = "알림")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name="notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "content")
    private String content;

    @Column(name = "is_read")
    @ColumnDefault("false")
    private boolean isRead;

    @Column(name = "target_id")
    private Long targetId;

    @Builder
    public Notification(User receiver, User sender, NotificationType notificationType, String content, Long targetId){
        this.receiver = receiver;
        this.sender = sender;
        this.notificationType = notificationType;
        this.content = content;
        this.targetId = targetId;
        this.isRead = false;
    }
}
