package LDHD.project.domain.group.web.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "댓글 삭제 이벤트 브로드캐스팅(동기화)")
public class CommentDeleteEvent {

    @Schema(description = "삭제된 댓글 ID")
    private Long commentId;

    @Schema(description = "댓글이 속한 문서 ID")
    private Long groupDocumentId;

    @Schema(description = "삭제 여부 (항상 true)")
    private boolean deleted;

    public static CommentDeleteEvent of(Long commentId, Long groupDocumentId) {
        return CommentDeleteEvent.builder()
                .commentId(commentId)
                .groupDocumentId(groupDocumentId)
                .deleted(true)
                .build();
    }
}
