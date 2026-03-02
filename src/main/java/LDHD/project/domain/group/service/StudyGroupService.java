package LDHD.project.domain.group.service;

import LDHD.project.common.aws.S3FileManager;
import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.utils.FileTextParser;
import LDHD.project.domain.chat.entity.GroupChatRoom;
import LDHD.project.domain.chat.repository.GroupChatRoomRepository;
import LDHD.project.domain.group.GroupRole;
import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.GroupMember;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.group.repository.StudyGroupRepository;
import LDHD.project.domain.group.web.dto.*;
import LDHD.project.domain.notification.event.GroupChatMemberInvitedEvent;
import LDHD.project.domain.notification.event.GroupMemberInvitedEvent;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupDocumentRepository groupDocumentRepository;
    private final UserRepository userRepository;
    private final S3FileManager s3FileManager;
    private final FileTextParser fileTextParser;
    private final ApplicationEventPublisher eventPublisher;
    private final GroupChatRoomRepository  groupChatRoomRepository;

    // 스터디 그룹 생성
    @Transactional
    public StudyGroupCreateResponse createGroup(Long userId, StudyGroupCreateRequest request){

        // 유저 조회
        User owner = userRepository.findById(userId).orElseThrow(
                ()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 그룹 생성
        StudyGroup group = StudyGroup.create(
                request.getName(),
                request.getDescription(),
                owner
        );

        studyGroupRepository.save(group);

        // 그룹 생성 시 채팅방 자동 생성
        GroupChatRoom chatRoom = GroupChatRoom.builder()
                .studyGroup(group)
                .build();

        groupChatRoomRepository.save(chatRoom);

        log.info("스터디 그룹 및 채팅방 자동 생성 완료 - groupId: {}, chatRoomId: {}", group.getId(), chatRoom.getId());

        return StudyGroupCreateResponse.from(group);
    }

    // 스터디 그룹 수정(이름, 설명)
    @Transactional
    public StudyGroupUpdateResponse updateStudyGroup(Long groupId, Long currentUserId, StudyGroupUpdateRequest request) {
        // 그룹 존재 확인
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 방장 확인
        if (!group.getOwner().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        group.update(request.getName(), request.getDescription());
        return StudyGroupUpdateResponse.from(group);
    }

    // 그룹에 학습 자료(문서) 추가
    @Transactional
    public GroupDocumentAddResponse addDocument(Long userId, Long groupId, GroupDocumentAddRequest request, MultipartFile file) {

        // 멤버 권한 검증
        if (!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, userId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }

        // 그룹 조회
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 업로더 조회
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 2. 파일 유효성 검사
        if(file.isEmpty()){throw new GeneralException(ErrorCode.VALIDATION_FAILED);}

        // userId 전달 → user/{userId}/uuid_file.pdf 경로로 저장 & key만 반환 (URL 아님)
        String s3Key = s3FileManager.upload(file, userId);
        String extractedText = fileTextParser.extractText(file);

        GroupDocument groupDocument = GroupDocument.create(
                group, uploader,
                request.getTitle(), request.getDescription(),
                s3Key, file.getOriginalFilename(), extractedText
        );

        groupDocumentRepository.save(groupDocument);
        return GroupDocumentAddResponse.from(groupDocument);
    }

    // 그룹 삭제
    @Transactional
    public void deleteStudyGroup(Long groupId, Long currentUserId) {

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwner().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        // GroupDocument에서 직접 s3Key 사용
        var documents = groupDocumentRepository.findAllByStudyGroupId(groupId);

        for (GroupDocument doc : documents) {
            s3FileManager.delete(doc.getS3Key());
        }

        deleteStudyGroupFromDb(group);
    }

    // DB에서 삭제
    @Transactional
    protected void deleteStudyGroupFromDb(StudyGroup group) {

        groupDocumentRepository.deleteAllByStudyGroup(group);
        groupMemberRepository.deleteAllByStudyGroup(group);
        studyGroupRepository.delete(group);
    }

    // 그룹 문서 수정 (업로더 본인만, title, description)
    @Transactional
    public GroupDocumentUpdateResponse updateGroupDocument(Long groupId, Long groupDocumentId, Long currentUserId,
                                                           GroupDocumentUpdateRequest request) {

        GroupDocument groupDocument = groupDocumentRepository
                .findByIdAndStudyGroupId(groupDocumentId, groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 업로더인지 확인
        if (!groupDocument.getUploader().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        groupDocument.update(request.getTitle(), request.getDescription());
        return GroupDocumentUpdateResponse.from(groupDocument);
    }

    // 그룹 문서 삭제 (업로더 본인 또는 방장만)
    @Transactional
    public void deleteGroupDocument(Long groupId, Long groupDocumentId, Long currentUserId) {

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 그룹 문서인지 검증
        GroupDocument groupDocument = groupDocumentRepository
                .findByIdAndStudyGroupId(groupDocumentId, groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 업로더 본인 또는 방장만 삭제 가능
        boolean isUploader = groupDocument.getUploader().getId().equals(currentUserId);
        boolean isOwner = group.getOwner().getId().equals(currentUserId);

        // 업로더 or 방장인지 확인
        if (!isUploader && !isOwner) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        // S3 파일 삭제
        s3FileManager.delete(groupDocument.getS3Key());
        groupDocumentRepository.delete(groupDocument);
    }

    // 그룹 문서 조회
    @Transactional(readOnly = true)
    public Page<GroupDocumentListResponse> getGroupDocuments(Long userId, Long groupId, Pageable pageable){

        // 그룹 존재 여부 검증
        if(!studyGroupRepository.existsById(groupId)){
            throw new GeneralException(ErrorCode.GROUP_NOT_FOUND);
        }
        // 멤버 권한 검증
        if(!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, userId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }
        // 문서 조회
        Page<GroupDocument> documents = groupDocumentRepository.findAllByStudyGroupId(groupId, pageable);

        // 업로더 ID 목록 한번에 추출
        List<Long> uploaderIds = documents.getContent().stream()
                .map(doc -> doc.getUploader().getId())
                .distinct()
                .collect(Collectors.toList());

        // 그룹 멤버인 업로더 ID Set으로 변환 (쿼리 1번)
        Set<Long> memberUploaderIds = groupMemberRepository
                .findAllByStudyGroup_IdAndUser_IdIn(groupId, uploaderIds)
                .stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        return documents.map(doc -> {
            boolean isUploaderInGroup = memberUploaderIds.contains(doc.getUploader().getId());
            return GroupDocumentListResponse.from(doc, isUploaderInGroup);
        });
    }

    // 사용가 속한 그룹 목록 조회 (createdAt 기준)
    public Page<GetStudyGroupListResponse> getMyGroups(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return studyGroupRepository.findAllByMemberId(userId, pageable)
                .map(GetStudyGroupListResponse::from);
    }
/*
    // StudyGroup 단건 조회 + 마지막 조회 시간 갱신(lastViewedAt 기준)
    @Transactional
    public GetStudyGroupListResponse getStudyGroup(Long groupId, Long currentUserId) {

        // 멤버 권한 검증
        if (!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, currentUserId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 조회 시 마지막 조회 시간 갱신
        group.updateLastViewedAt();

        return GetStudyGroupListResponse.from(group);
    }
    // 최근 조회한 순서로 그룹 목록 반환
    public Page<GetStudyGroupListResponse> getRecentViewedGroups(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return studyGroupRepository.findAllByMemberIdOrderByLastViewedAt(userId, pageable)
                .map(GetStudyGroupListResponse::from);
    }
    */
    // 홈 화면 - 가장 최근 조회한 StudyGroup 1개 반환
    public GetStudyGroupListResponse getLatestViewedStudyGroup(Long userId) {

        return studyGroupRepository
                .findTopByMembers_User_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(userId)
                .map(GetStudyGroupListResponse::from)
                .orElse(null); // 한 번도 조회 안 했으면 null
    }

    @Transactional
    // 그룹 문서 파일 단건 조회
    public GroupFileResponse getGroupFile(Long groupId, Long groupDocumentId, Long currentUserId) {

        // 멤버 권한 검증
        if (!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, currentUserId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }

        // 해당 그룹의 문서인지 함께 검증
        GroupDocument groupDocument = groupDocumentRepository.findByIdAndStudyGroupId(groupDocumentId, groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 파일 조회 시 StudyGroup lastViewedAt 갱신
        StudyGroup group = groupDocument.getStudyGroup();
        group.updateLastViewedAt();

        // Presigned URL 생성 후 반환
        String presignedUrl = s3FileManager.generatePresignedUrl(groupDocument.getS3Key());

        // 업로더가 현재 그룹 멤버인지 확인
        boolean isUploaderInGroup = groupMemberRepository
                .existsByStudyGroupIdAndUserId(groupId, groupDocument.getUploader().getId());

        return GroupFileResponse.from(groupDocument, presignedUrl, isUploaderInGroup);
    }

    // 스터디 그룹에 멤버 초대
    @Transactional
    public GroupMemberResponse inviteMember(Long groupId, Long currentUserId,
                                            GroupMemberInviteRequest request) {
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        if (!group.getOwner().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        if (groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, request.getUserId())) {
            throw new GeneralException(ErrorCode.DUPLICATE_GROUP_MEMBER);
        }

        User invitedUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        User inviter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        GroupMember newMember = groupMemberRepository.save(
                GroupMember.builder()
                        .user(invitedUser)
                        .studyGroup(group)
                        .role(GroupRole.MEMBER)
                        .build()
        );
        // 그룹 멤버 초대 알림 이벤트 발행
        eventPublisher.publishEvent(new GroupMemberInvitedEvent(
                groupId,
                inviter.getName(),
                List.of(request.getUserId())
        ));
        // 그룹 채팅방 존재 여부 확인 후 채팅방 초대 알림 전송
        groupChatRoomRepository.findByStudyGroup_Id(groupId)
                .ifPresent(chatRoom ->
                        eventPublisher.publishEvent(new GroupChatMemberInvitedEvent(
                                chatRoom.getId(),
                                inviter.getName(),
                                List.of(request.getUserId())
                        ))
                );


        return GroupMemberResponse.from(newMember);
    }

    // 스터디 그룹에서 본인 탈퇴(방 나가기) + 탈퇴 시 본인이 올린 파일 삭제
    @Transactional
    public void leaveGroup(Long groupId, Long currentUserId) {

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 방장은 탈퇴 불가
        if (group.getOwner().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.INVALID_REQUEST);
        }

        GroupMember member = groupMemberRepository
                .findByStudyGroup_IdAndUser_Id(groupId, currentUserId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_GROUP_MEMBER));

        /*
        // 본인이 올린 그룹 문서 조회
        List<GroupDocument> myDocuments =
                groupDocumentRepository.findAllByStudyGroupIdAndUploaderId(groupId, currentUserId);

        // S3 파일 삭제
        for (GroupDocument doc : myDocuments) {
            s3FileManager.delete(doc.getS3Key());
        }

        // GroupDocument DB 삭제
        groupDocumentRepository.deleteAll(myDocuments);


         */
        // 멤버 삭제
        groupMemberRepository.delete(member);
    }
}
