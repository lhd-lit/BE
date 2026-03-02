package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupDocument;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupFileResponse {


    private Long uploaderId;
    private String title;
    private String description;
    private String presignedUrl;
    private String uploaderName;

    public static GroupFileResponse from(GroupDocument document, String presignedUrl, boolean isUploaderInGroup) {

        return GroupFileResponse.builder()
                .title(document.getTitle())
                .description(document.getDescription())
                .presignedUrl(presignedUrl)
                .uploaderId(isUploaderInGroup ? document.getUploader().getId() : null)
                .uploaderName(isUploaderInGroup ? document.getUploader().getName() : null)
                .build();
    }
}
