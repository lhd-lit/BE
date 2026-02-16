package LDHD.project.domain.group.web.dto;

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
    private Long selfStudyId;
    private String title;
    private String description;
    private String uploaderName;

    public static GroupDocumentListResponse from(GroupDocument groupDocument) {
        return GroupDocumentListResponse.builder()
                .groupDocumentId(groupDocument.getId())
                .selfStudyId(groupDocument.getSelfStudy().getId())
                .title(groupDocument.getSelfStudy().getTitle())
                .description(groupDocument.getSelfStudy().getDescription())
                .uploaderName(groupDocument.getSelfStudy().getUploader().getName())
                .build();
    }
}
