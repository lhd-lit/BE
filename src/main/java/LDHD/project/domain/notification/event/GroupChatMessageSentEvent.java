package LDHD.project.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class GroupChatMessageSentEvent {

    private final Long chatRoomId;
    private final Long senderId;
    private final String senderName;
    private final List<Long> memberIds;
}
