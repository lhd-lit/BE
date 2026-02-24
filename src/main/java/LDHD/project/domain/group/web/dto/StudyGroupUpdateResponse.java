package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.StudyGroup;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyGroupUpdateResponse {

    private Long studyGroupId;
    private String name;
    private String description;

    public static StudyGroupUpdateResponse from(StudyGroup group) {
        return StudyGroupUpdateResponse.builder()
                .studyGroupId(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .build();
    }
}
