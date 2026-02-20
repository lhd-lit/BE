package LDHD.project.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class GroupChatRoomCreatedEvent {

    private final Long chatRoomId;          // 생성된 채팅방 ID
    private final Long studyGroupId;        // 소속 스터디 그룹 ID
    private final String studyGroupName;    // 그룹명 (알림 메시지용)
    private final List<Long> memberIds;     // 알림 수신 대상 멤버 ID 목록
    private final Long creatorId;           // 생성자 ID (본인 제외용)
}
