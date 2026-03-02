package LDHD.project.domain.group.web.dto;

import LDHD.project.domain.group.entity.GroupMember;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupMemberResponse {

    private Long memberId;
    private Long userId;
    private String userName;
    private String role;

    public static GroupMemberResponse from(GroupMember member) {
        return GroupMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .userName(member.getUser().getName())
                .role(member.getRole().name())
                .build();
    }
}
