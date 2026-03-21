package LDHD.project.domain.group.web.dto.comment;

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
@Schema(description = "대댓글 생성 요청")
public class CreateReplyRequest {

    @NotNull(message = "문서 ID는 필수입니다.")
    @Schema(description = "댓글이 속한 문서 ID")
    private Long groupDocumentId;

    @NotNull(message = "부모 댓글 ID는 필수입니다.")
    @Schema(description = "대댓글을 달 루트 댓글 ID")
    private Long parentCommentId;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 255, message = "댓글은 255자를 넘을 수 없습니다.")
    @Schema(description = "대댓글 내용")
    private String content;

}
