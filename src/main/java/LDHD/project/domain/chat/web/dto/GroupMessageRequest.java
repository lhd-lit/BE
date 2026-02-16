package LDHD.project.domain.chat.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GroupMessageRequest {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 500, message = "메시지는 500자를 넘을 수 없습니다.")
    private String content;
}
