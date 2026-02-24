package LDHD.project.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class GroupMemberInvitedEvent {

    private final Long studyGroupId;       // 이동 대상 (그룹 ID)
    private final String inviterName;      // 초대자
    private final List<Long> invitedUserIds; // 초대된 사용자 목록
}
