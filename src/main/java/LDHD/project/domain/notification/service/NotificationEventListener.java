package LDHD.project.domain.notification.service;

import LDHD.project.domain.notification.NotificationType;
import LDHD.project.domain.notification.event.GroupChatMemberInvitedEvent;
import LDHD.project.domain.notification.event.GroupChatMessageSentEvent;
import LDHD.project.domain.notification.event.GroupChatRoomCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    // 채팅방 생성 시 이벤트 알림 수신 => 그룹 멤버 전체에게 알림
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋 후 발행 보장(DB저장 실패, 알림 발행 방지)
    public void handleGroupChatRoomCreated(GroupChatRoomCreatedEvent event) {
        log.debug("채팅방 알림 수신 - chatroomId: {}", event.getChatRoomId());

        // 생성자 본인 제외
        List<Long> targetIds = event.getMemberIds().stream()
                .filter(id -> !id.equals(event.getCreatorId())) // filter -> 생성자 본인은 제외
                .collect(Collectors.toList());

        if (targetIds.isEmpty()) return;

        String message = "[" + event.getStudyGroupName() + "] " +
                NotificationType.GROUP_CHAT_ROOM_CREATED.getMessageTemplate();

        notificationService.sendNotifications(
                targetIds,
                NotificationType.GROUP_CHAT_ROOM_CREATED,
                message,
                event.getChatRoomId()
        );
    }

    // 채팅방 초대 이벤트 수신 => 초대된 사용자들에게 알림
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupChatMemberInvited(GroupChatMemberInvitedEvent event) {
        log.debug("멤버 초대 이벤트 수신 - chatRoomId: {}, 초대 인원: {}명",
                event.getChatRoomId(), event.getInvitedUserIds().size());

        String message = NotificationType.GROUP_CHAT_MEMBER_INVITED
                .formatMessage(event.getInviterName());

        notificationService.sendNotifications(
                event.getInvitedUserIds(),
                NotificationType.GROUP_CHAT_MEMBER_INVITED,
                message,
                event.getChatRoomId()
        );
    }


    // 채팅 메시지 전송 이벤트 수신 => 발신자 제외 채팅방 멤버 전원에게 알림
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupChatMessageSent(GroupChatMessageSentEvent event) {
        log.debug("메시지 전송 이벤트 수신 - chatRoomId: {}", event.getChatRoomId());

        // 발신자 본인 제외
        List<Long> targetIds = event.getMemberIds().stream()
                .filter(id -> !id.equals(event.getSenderId()))
                .collect(Collectors.toList());

        if (targetIds.isEmpty()) return;

        String message = NotificationType.GROUP_CHAT_MESSAGE_RECEIVED
                .formatMessage(event.getSenderName());

        notificationService.sendNotifications(
                targetIds,
                NotificationType.GROUP_CHAT_MESSAGE_RECEIVED,
                message,
                event.getChatRoomId()
        );
    }
}
