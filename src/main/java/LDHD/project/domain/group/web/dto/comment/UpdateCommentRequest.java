package LDHD.project.domain.group.web.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "댓글 수정 요청")
public class UpdateCommentRequest {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 255, message = "댓글은 255자를 넘을 수 없습니다.")
    @Schema(description = "수정할 댓글 내용")
    private String content;

}
