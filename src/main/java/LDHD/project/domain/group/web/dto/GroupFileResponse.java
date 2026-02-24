package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupDocument;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupFileResponse {


    private final String title;
    private final String description;
    private final String presignedUrl;


    public static GroupFileResponse from(GroupDocument document, String presignedUrl) {
        return GroupFileResponse.builder()
                .title(document.getTitle())
                .description(document.getDescription())
                .presignedUrl(presignedUrl)
                .build();
    }
}
