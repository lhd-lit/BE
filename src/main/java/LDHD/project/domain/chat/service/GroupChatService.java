package LDHD.project.domain.chat.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.chat.entity.GroupChatMessage;
import LDHD.project.domain.chat.entity.GroupChatRoom;
import LDHD.project.domain.chat.repository.GroupChatMessageRepository;
import LDHD.project.domain.chat.repository.GroupChatRoomRepository;
import LDHD.project.domain.chat.web.dto.GroupMessageRequest;
import LDHD.project.domain.chat.web.dto.GroupMessageResponse;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupChatService {

    private final GroupChatMessageRepository messageRepository;
    private final GroupChatRoomRepository roomRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupMessageResponse sendMessage(Long userId, GroupMessageRequest request) {

        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 2. 채팅방 존재 확인
        GroupChatRoom chatRoom = roomRepository.findById(request.getChatRoomId()).orElseThrow(
                ()-> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

        // 3. 멤버십 검증
        Long studyGroupId = chatRoom.getStudyGroup().getId();

        if(!memberRepository.existsByStudyGroupIdAndUserId(studyGroupId, userId)){
            log.warn("Unauthorized chat access - userId: {}, studyGroupId: {} ", userId, studyGroupId);
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }
        // 4. 텍스트 제외 입력값 인코딩 -> 수신자 보호 차원
        // 사용자 입력값에서 HTML 태그 이스케이프 처리
        String safeContent = HtmlUtils.htmlEscape(request.getContent());

        // 5. 메시지 생성 및 저장
        GroupChatMessage message = GroupChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user)
                .content(safeContent)
                .build();

        messageRepository.save(message);
        log.info("Group message saved - messageId: {}, chatRoomId: {}",
                message.getId(), chatRoom.getId());

        return GroupMessageResponse.from(message);
    }

    // 채팅 메시지 이력 조회(커서 페이징) - cursor가 null : 최신 메시지부터 / 값 존재: 해당 시간 이전
    public List<GroupMessageResponse> getMessageHistory(
            Long chatRoomId,
            LocalDateTime cursor,
            int size
    )
    {
        Pageable pageable = PageRequest.of(
                0,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Slice<GroupChatMessage> slice;

        if(cursor == null){ // 최신 메시지 조회
        slice = messageRepository.findByChatRoom_IdOrderByCreatedAtDesc(
                chatRoomId,
                pageable
        );
        } else { // 이전 메시지 조회
            slice = messageRepository.findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    chatRoomId,
                    cursor,
                    pageable
            );
        }

        List<GroupMessageResponse> messages = slice.getContent()
                .stream()
                .map(GroupMessageResponse::from)
                .collect(Collectors.toList());

        log.debug("Message history retrieved - chatRoomId: {}, count: {}",
                chatRoomId, messages.size());

        return messages;
    }

    // 전체 메시지 조회
    public List<GroupMessageResponse> getAllMessages(Long chatRoomId) {
        List<GroupChatMessage> messages = messageRepository.findAllByChatRoomId(chatRoomId);

        return messages.stream()
                .map(GroupMessageResponse::from)
                .collect(Collectors.toList());
    }

    // 읽지 않은 메시지 수 조회 (countNewMessages : 새 메시지 개수)
    public Long getUnreadMessageCount(Long chatRoomId, LocalDateTime lastReadTime) {
        return messageRepository.countNewMessages(chatRoomId, lastReadTime);
    }
}
