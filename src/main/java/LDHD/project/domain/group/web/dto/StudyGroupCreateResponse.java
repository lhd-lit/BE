package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.StudyGroup;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyGroupCreateResponse {

    private Long id;
    private String name;
    private String description;
    private String ownerName;

    public static StudyGroupCreateResponse from(StudyGroup group) {
        return StudyGroupCreateResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .ownerName(group.getOwner().getName())
                .build();
    }
}
