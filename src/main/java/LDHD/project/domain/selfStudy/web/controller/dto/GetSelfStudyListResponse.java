package LDHD.project.domain.selfStudy.web.controller.dto;

import LDHD.project.domain.selfStudy.SelfStudy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSelfStudyListResponse {

    Long selfStudyId;
    String title;
    String description;
    String originalFileName;
    String writerName;
    LocalDateTime lastViewedAt;

    public static GetSelfStudyListResponse from(SelfStudy selfStudy) {
        return GetSelfStudyListResponse.builder()
                .selfStudyId(selfStudy.getId())
                .title(selfStudy.getTitle())
                .description(selfStudy.getDescription())
                .originalFileName(selfStudy.getOriginalFileName())
                .writerName(selfStudy.getUploader().getName())
                .lastViewedAt(selfStudy.getLastViewedAt())
                .build();
    }
}
