package LDHD.project.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class GroupChatMemberInvitedEvent {

    private final Long chatRoomId;
    private final String inviterName;
    private final List<Long> invitedUserIds;
}
