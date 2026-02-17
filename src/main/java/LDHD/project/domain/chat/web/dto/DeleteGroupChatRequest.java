package LDHD.project.domain.chat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "그룹 채팅방 삭제 요청 DTO")
public class DeleteGroupChatRequest {

    @NotNull(message = "삭제할 채팅방 ID는 필수입니다.")
    @Schema(description = "삭제할 채팅방 ID")
    private Long chatRoomId;
}
