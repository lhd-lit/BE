package LDHD.project.domain.chat.web.dto;

import LDHD.project.domain.chat.entity.GroupChatMessage;
import LDHD.project.domain.chat.entity.GroupChatRoom;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "그룹 채팅 메시지 응답 DTO")
public class GroupMessageResponse {

    private Long messageId;
    private Long chatRoomId;
    private Long senderId;

    private String senderName;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static GroupMessageResponse from(GroupChatMessage message) {
        return GroupMessageResponse.builder()
                .messageId(message.getId())
                .chatRoomId(message.getChatRoom().getId())  // [추가]
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
