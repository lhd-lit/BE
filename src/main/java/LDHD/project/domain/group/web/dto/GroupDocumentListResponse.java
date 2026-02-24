package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDocumentListResponse { // 조회용 dto

    private Long groupDocumentId;
    private String title;
    private String description;
    private String uploaderName;
    private String originalFileName;

    public static GroupDocumentListResponse from(GroupDocument groupDocument) {
        return GroupDocumentListResponse.builder()
                .groupDocumentId(groupDocument.getId())
                .title(groupDocument.getTitle())
                .description(groupDocument.getDescription())
                .uploaderName(groupDocument.getUploader().getName())
                .originalFileName(groupDocument.getOriginalFileName())
                .build();
    }
}
