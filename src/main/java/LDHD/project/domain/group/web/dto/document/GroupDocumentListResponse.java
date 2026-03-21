package LDHD.project.domain.group.web.dto.document;

import LDHD.project.domain.group.entity.GroupDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDocumentListResponse { // 조회용 dto

    private Long groupDocumentId;
    private Long uploaderId;
    private String title;
    private String description;
    private String uploaderName;
    private String originalFileName;

    public static GroupDocumentListResponse from(GroupDocument groupDocument, boolean isUploaderInGroup) {
        return GroupDocumentListResponse.builder()
                .groupDocumentId(groupDocument.getId())
                .title(groupDocument.getTitle())
                .description(groupDocument.getDescription())
                .originalFileName(groupDocument.getOriginalFileName())
                .uploaderId(isUploaderInGroup ? groupDocument.getUploader().getId() : null)
                .uploaderName(isUploaderInGroup ? groupDocument.getUploader().getName() : null)
                .build();
    }
}
