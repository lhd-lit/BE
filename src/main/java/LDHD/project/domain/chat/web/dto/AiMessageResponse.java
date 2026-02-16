package LDHD.project.domain.chat.web.dto;

import LDHD.project.domain.chat.AiMessageRole;
import LDHD.project.domain.chat.entity.AiChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AiMessageResponse {

    private Long messageId;
    private Long chatRoomId;

    private AiMessageRole role;

    private Long senderId;

    private String content;

    // 날짜 형식 지정
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // senderId: USER - userId, Ai - null
    public static AiMessageResponse from(AiChatMessage message, Long senderId) {
        return AiMessageResponse.builder()
                .messageId(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .role(message.getRole())
                .senderId(senderId)
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
