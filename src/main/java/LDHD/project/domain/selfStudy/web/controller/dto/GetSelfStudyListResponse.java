package LDHD.project.domain.selfStudy.web.controller.dto;

import LDHD.project.domain.selfStudy.SelfStudy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSelfStudyListResponse {
    Long id;
    String title;
    String description;
    String fileUrl;
    String original_file_name;
    String writerName;

    public static GetSelfStudyListResponse from(SelfStudy selfStudy) {
        return GetSelfStudyListResponse.builder()
                .id(selfStudy.getId())
                .title(selfStudy.getTitle())
                .description(selfStudy.getDescription())
                .fileUrl(selfStudy.getFileUrl())
                .original_file_name(selfStudy.getOriginalFileName())
                .writerName(selfStudy.getUploader().getName())
                .build();
    }
}
