package LDHD.project.domain.chat.web.dto;

import LDHD.project.domain.chat.entity.AiChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAiChatRoomListResponse {

    private Long roomId;
    private String lastMessage;
    private LocalDateTime createdAt;

    public static GetAiChatRoomListResponse from(AiChatRoom room) {
        return GetAiChatRoomListResponse.builder()
                .roomId(room.getId())
                .lastMessage(room.toString())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
