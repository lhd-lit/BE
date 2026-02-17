package LDHD.project.domain.chat.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.chat.entity.GroupChatMessage;
import LDHD.project.domain.chat.entity.GroupChatRoom;
import LDHD.project.domain.chat.repository.GroupChatMessageRepository;
import LDHD.project.domain.chat.repository.GroupChatRoomRepository;
import LDHD.project.domain.chat.web.dto.*;
import LDHD.project.domain.group.GroupRole;
import LDHD.project.domain.group.entity.GroupMember;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.group.repository.StudyGroupRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
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
    private final StudyGroupRepository studyGroupRepository;

    // 그룹 채팅방 생성
    @Transactional(readOnly = false)
    public GroupChatRoomResponse createChatRoom(CreateGroupChatRequest request, Long requesterId) {

        StudyGroup studyGroup = studyGroupRepository.findById(request.getStudyGroupId())
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 권한 체크
        if (!memberRepository.existsByStudyGroup_IdAndUser_Id(studyGroup.getId(), requesterId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        // 1차 체크
        if (roomRepository.findByStudyGroup_Id(studyGroup.getId()).isPresent()) {
            throw new GeneralException(ErrorCode.ALREADY_EXISTS);
        }

        try {
            // 2차 체크 (DB 레벨 Unique Constraint)
            GroupChatRoom savedRoom = roomRepository.save(
                    GroupChatRoom.builder()
                            .studyGroup(studyGroup)
                            .build()
            );

            // 초기 멤버 수 계산 (쿼리 최적화 할 필요 없이 단건이므로 count)
            // 보통 그룹 생성자가 채팅방을 만들므로 최소 1명 이상
            long countResult = memberRepository.countByStudyGroup_Id(studyGroup.getId());
            int memberCount = (int) countResult;

            return GroupChatRoomResponse.of(savedRoom, memberCount);

        } catch (DataIntegrityViolationException e) {
            // 동시성 이슈로 인한 중복 생성 시도 시 예외 전환
            throw new GeneralException(ErrorCode.ALREADY_EXISTS);
        }
    }

    // 사용자 초대(이메일 기반)
    @Transactional(readOnly = false)
    public void inviteUsers(Long studyGroupId, InviteGroupChatMemberRequest request, Long inviterId) {

        // 초대자 권한 검증
        if (!memberRepository.existsByStudyGroup_IdAndUser_Id(studyGroupId, inviterId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        StudyGroup studyGroup = studyGroupRepository.findById(studyGroupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        List<Long> targetIds = request.getUserIds();
        List<User> usersToInvite = userRepository.findAllByIdIn(targetIds);

        if (usersToInvite.isEmpty()) {
            throw new GeneralException(ErrorCode.USER_NOT_FOUND);
        }

        // 이미 멤버인 사용자 필터링
        List<GroupMember> existingMembers = memberRepository.findAllByStudyGroup_IdAndUser_IdIn(studyGroupId, targetIds);

        Set<Long> existingUserIds = existingMembers.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        List<GroupMember> newMembers = usersToInvite.stream()
                .filter(user -> !existingUserIds.contains(user.getId()))
                .map(user -> GroupMember.builder()
                        .user(user)
                        .studyGroup(studyGroup)
                        .role(GroupRole.MEMBER)
                        .build())
                .collect(Collectors.toList());

        if (!newMembers.isEmpty()) {
            memberRepository.saveAll(newMembers);
            log.info("그룹[{}] 초대 완료: {}명", studyGroup.getName(), newMembers.size());
        }
    }

    // 사용자의 그룹 채팅방 목록 조회
    public Page<GroupChatRoomResponse> getMyChatRooms(Long userId, Pageable pageable) {

        // ID만 페이징으로 조회
        Page<Long> roomIdsPage = roomRepository.findRoomIdsByUserId(userId, pageable);
        List<Long> roomIds = roomIdsPage.getContent();

        if (roomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 조회된 ID로 Entity 조회 (Fetch Join으로 StudyGroup 로딩)
        List<GroupChatRoom> rooms = roomRepository.findAllByIdIn(roomIds);

        // ID 리스트 순서(정렬)를 유지하기 위해 Map으로 변환
        Map<Long, GroupChatRoom> roomMap = rooms.stream()
                .collect(Collectors.toMap(GroupChatRoom::getId, Function.identity()));

        // 그룹별 멤버 수 일괄 조회 (N+1 방지)
        List<Long> groupIds = rooms.stream()
                .map(r -> r.getStudyGroup().getId())
                .collect(Collectors.toList());

        Map<Long, Integer> memberCountMap = memberRepository.countMembersByStudyGroupIdsIn(groupIds)
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],       // key: studyGroupId
                        obj -> ((Long) obj[1]).intValue() // value: count
                ));

        // 최종 DTO 변환 (Page의 ID 순서대로 매핑하여 정렬 유지)
        List<GroupChatRoomResponse> content = roomIds.stream()
                .map(roomMap::get)
                .map(room -> {
                    Integer count = memberCountMap.getOrDefault(room.getStudyGroup().getId(), 0);
                    return GroupChatRoomResponse.of(room, count);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, roomIdsPage.getTotalElements());
    }

    // 그룹 채팅방 삭제
    @Transactional(readOnly = false)
    public void deleteChatRoom(DeleteGroupChatRequest request, Long requesterId) {

        Long chatRoomId = request.getChatRoomId();
        GroupChatRoom chatRoom = roomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

        // 방장(LEADER) 권한 확인
        boolean isLeader = memberRepository.findByStudyGroup_IdAndUser_Id(chatRoom.getStudyGroup().getId(), requesterId)
                .map(member -> member.getRole() == GroupRole.LEADER)
                .orElse(false);

        if (!isLeader) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        roomRepository.delete(chatRoom);
        log.info("그룹 채팅방 삭제 완료 - id: {}, requester: {}", chatRoomId, requesterId);
    }

    // 메시지 전송
    @Transactional(readOnly = false)
    public GroupMessageResponse sendMessage(Long userId, GroupMessageRequest request) {

        // 권한 검증
        GroupChatRoom chatRoom = validateGroupMember(request.getChatRoomId(), userId);

        // 1. 사용자 존재 확인
        User user = userRepository.findById(userId).orElseThrow(
                () -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 2. 텍스트 제외 입력값 인코딩 -> 수신자 보호 차원
        // 사용자 입력값에서 HTML 태그 이스케이프 처리
        String safeContent = HtmlUtils.htmlEscape(request.getContent());

        // 3. 메시지 생성 및 저장
        GroupChatMessage message = GroupChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user)
                .content(safeContent)
                .build();

        messageRepository.save(message);
        log.info("그룹 메시지 저장 완료 - messageId: {}, chatRoomId: {}",
                message.getId(), chatRoom.getId());

        return GroupMessageResponse.from(message);
    }

    // 채팅 메시지 이력 조회(커서 페이징) - cursor가 null : 최신 메시지부터 / 값 존재: 해당 시간 이전
    public List<GroupMessageResponse> getMessageHistory(Long userId, Long chatRoomId, LocalDateTime cursor, int size) {

        // 권한 검증
        validateGroupMember(chatRoomId, userId);

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Slice<GroupChatMessage> slice;

        if (cursor == null) { // 최신 메시지 조회
            slice = messageRepository.findByChatRoom_IdOrderByCreatedAtDesc(chatRoomId, pageable);
        } else { // 이전 메시지 조회
            slice = messageRepository.findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(chatRoomId, cursor, pageable);
        }

        // DESC 조회 후 reverse : 클라이언트에 과거→최신 순 반환
        List<GroupChatMessage> messages = new ArrayList<>(slice.getContent());
        Collections.reverse(messages);

        return messages.stream()
                .map(GroupMessageResponse::from)
                .collect(Collectors.toList());

    }

    // 전체 메시지 조회
    public List<GroupMessageResponse> getAllMessages(Long chatRoomId, Long userId) {

        // 권한 검증
        validateGroupMember(chatRoomId, userId);

        List<GroupChatMessage> messages = messageRepository.findAllByChatRoomId(chatRoomId);

        return messages.stream()
                .map(GroupMessageResponse::from)
                .collect(Collectors.toList());
    }

    // 읽지 않은 메시지 수 조회 (countNewMessages : 새 메시지 개수)
    public Long getUnreadMessageCount(Long chatRoomId, Long userId, LocalDateTime lastReadTime) {

        validateGroupMember(chatRoomId, userId);

        return messageRepository.countNewMessages(chatRoomId, lastReadTime);
    }

    // 권한 검증 (그룹 채팅방 존재 + 사용자 멤버십 검증)
    private GroupChatRoom validateGroupMember(Long chatRoomId, Long userId) {

        // 1. 채팅방 존재 확인
        GroupChatRoom chatRoom = roomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorCode.CHATROOM_NOT_FOUND));

        // 2. 해당 채팅방이 속한 그룹 ID 조회
        Long studyGroupId = chatRoom.getStudyGroup().getId();

        // 3. 사용자 멤버십 검증
        if (!memberRepository.existsByStudyGroup_IdAndUser_Id(studyGroupId, userId)) {
            log.warn("Unauthorized group chat access - userId: {}, chatRoomId: {}",
                    userId, chatRoomId);
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        return chatRoom;
    }

}
