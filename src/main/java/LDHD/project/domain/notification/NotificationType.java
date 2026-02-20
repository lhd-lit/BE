package LDHD.project.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // 그룹 채팅방 생성
    GROUP_CHAT_ROOM_CREATED("새로운 그룹 채팅방이 생성되었습니다."),

    // 그룹 채팅방 초대
    GROUP_CHAT_MEMBER_INVITED("%s님이 그룹 채팅방에 초대했습니다."),

    // 그룹 채팅 메시지 수신
    GROUP_CHAT_MESSAGE_RECEIVED("%s님이 메시지를 보냈습니다.");

    private final String messageTemplate;

    public String formatMessage(Object... args) {
        return String.format(messageTemplate, args);
    }
}
