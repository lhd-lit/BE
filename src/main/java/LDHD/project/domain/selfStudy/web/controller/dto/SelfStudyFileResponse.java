package LDHD.project.domain.selfStudy.web.controller.dto;

import LDHD.project.domain.selfStudy.SelfStudy;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SelfStudyFileResponse {

    private final String title;
    private final String description;
    private final String presignedUrl;

    public static SelfStudyFileResponse of(SelfStudy selfStudy, String presignedUrl) {

        return SelfStudyFileResponse.builder()
                .title(selfStudy.getTitle())
                .description(selfStudy.getDescription())
                .presignedUrl(presignedUrl)
                .build();
    }
}
