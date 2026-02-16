package LDHD.project.domain.chat.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.chat.AiMessageRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_chat_message")
public class AiChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private AiChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AiMessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder
    public AiChatMessage(Long id, AiChatRoom chatRoom, AiMessageRole role, String content) {
        this.chatRoom = chatRoom;
        this.role = role;
        this.content = content;
    }
}
