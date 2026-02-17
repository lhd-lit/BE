package LDHD.project.domain.chat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "그룹 채팅 메시지 전송 요청 DTO")
public class GroupMessageRequest {
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 500, message = "메시지는 500자를 넘을 수 없습니다.")
    private String content;
}
