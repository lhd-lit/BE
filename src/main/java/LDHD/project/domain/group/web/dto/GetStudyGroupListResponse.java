package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GetStudyGroupListResponse {

    private final Long studyGroupId;
    private final String name;
    private final String description;
    private final String ownerName;
    private final int memberCount;
    private final LocalDateTime lastViewedAt;

    public static GetStudyGroupListResponse from(StudyGroup group) {
        return GetStudyGroupListResponse.builder()
                .studyGroupId(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .ownerName(group.getOwner().getName())
                .memberCount(group.getMembers().size())
                .lastViewedAt(group.getLastViewedAt())
                .build();
    }
}
