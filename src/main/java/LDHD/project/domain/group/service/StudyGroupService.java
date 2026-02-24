package LDHD.project.domain.group.service;

import LDHD.project.common.aws.S3FileManager;
import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.utils.FileTextParser;
import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.group.repository.StudyGroupRepository;
import LDHD.project.domain.group.web.dto.*;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupDocumentRepository groupDocumentRepository;
    private final UserRepository userRepository;
    private final S3FileManager s3FileManager;
    private final FileTextParser fileTextParser;
    private final ApplicationEventPublisher applicationEventPublisher;

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

        return documents.map(GroupDocumentListResponse::from);
    }

    // 사용가 속한 그룹 목록 조회 (createdAt 기준)
    public Page<GetStudyGroupListResponse> getMyGroups(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return studyGroupRepository.findAllByMemberId(userId, pageable)
                .map(GetStudyGroupListResponse::from);
    }

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

        return GroupFileResponse.from(groupDocument, presignedUrl);
    }

}
