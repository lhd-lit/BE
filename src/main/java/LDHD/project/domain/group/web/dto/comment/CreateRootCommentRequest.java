package LDHD.project.domain.group.web.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "문서 루트 댓글 생성 요청")
public class CreateRootCommentRequest {

    @NotNull(message = "문서 ID는 필수입니다.")
    @Schema(description = "댓글을 달 문서 ID")
    private Long groupDocumentId;

    @NotBlank(message = "선택한 텍스트는 필수입니다.")
    @Schema(description = "사용자가 드래그한 텍스트 원문")
    private String highlightedText;

    @NotNull(message = "텍스트 시작 위치는 필수입니다.")
    @Schema(description = "Selection API anchorOffset (드래그 시작 위치)")
    private Integer anchorOffset;

    @NotNull(message = "텍스트 끝 위치는 필수입니다.")
    @Schema(description = "Selection API focusOffset (드래그 끝 위치)")
    private Integer focusOffset;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 255, message = "댓글은 255자를 넘을 수 없습니다.")
    @Schema(description = "댓글 내용")
    private String content;
}
