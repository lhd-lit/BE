package LDHD.project.domain.group.web.dto.comment;

import LDHD.project.domain.group.entity.DocumentComment;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "문서 댓글 응답")
public class DocumentCommentResponse {

    @Schema(description = "댓글 ID")
    private Long commentId;

    @Schema(description = "문서 ID")
    private Long groupDocumentId;

    @Schema(description = "작성자 ID")
    private Long authorId;

    @Schema(description = "작성자 이름")
    private String authorName;

    @Schema(description = "선택한 텍스트 원문 (루트 댓글만 존재, 대댓글은 null)")
    private String highlightedText;

    @Schema(description = "anchorOffset (루트 댓글만 존재)")
    private Integer anchorOffset;

    @Schema(description = "focusOffset (루트 댓글만 존재)")
    private Integer focusOffset;

    @Schema(description = "댓글 내용")
    private String content;

    @Schema(description = "부모 댓글 ID (대댓글인 경우만 존재, 루트는 null)")
    private Long parentCommentId;

    @Schema(description = "대댓글 목록 (루트 댓글만 존재)")
    private List<DocumentCommentResponse> replies;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "작성 시간")
    private LocalDateTime createdAt;

    public static DocumentCommentResponse from(DocumentComment comment) {

        return DocumentCommentResponse.builder()
                .commentId(comment.getId())
                .groupDocumentId(comment.getGroupDocument().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .highlightedText(comment.getHighlightedText())
                .anchorOffset(comment.getAnchorOffset())
                .focusOffset(comment.getFocusOffset())
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment() != null
                        ? comment.getParentComment().getId() : null)
                .replies(comment.getReplies() != null
                        ? comment.getReplies().stream()
                        .map(DocumentCommentResponse::from)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
