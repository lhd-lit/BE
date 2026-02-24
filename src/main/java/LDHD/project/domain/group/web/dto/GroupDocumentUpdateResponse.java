package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupDocument;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupDocumentUpdateResponse {

    private Long groupDocumentId;
    private String title;
    private String description;

    public static GroupDocumentUpdateResponse from(GroupDocument document) {

        return GroupDocumentUpdateResponse.builder()
                .groupDocumentId(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .build();
    }
}
