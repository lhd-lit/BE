package LDHD.project.domain.group.service;

import LDHD.project.common.aws.S3FileManager;
import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.group.repository.StudyGroupRepository;
import LDHD.project.domain.group.web.dto.*;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupDocumentRepository groupDocumentRepository;
    private final SelfStudyRepository selfStudyRepository;
    private final UserRepository userRepository;
    private final S3FileManager s3FileManager;

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

    // 그룹에 학습 자료(문서) 추가
    @Transactional
    public GroupDocumentAddResponse addDocument(Long userId, Long groupId, GroupDocumentAddRequest request){

        // 멤버 권한 검증
        if (!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, userId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }

        // 그룹 조회
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.GROUP_NOT_FOUND));

        // 학습 자료 조회
        SelfStudy selfStudy = selfStudyRepository.findById(request.getSelfStudyId())
                .orElseThrow(() -> new GeneralException(ErrorCode.SELF_STUDY_NOT_FOUND));

        // 문서 중복 검증
        if (groupDocumentRepository.existsByStudyGroupAndSelfStudy(group, selfStudy)) {
            throw new GeneralException(ErrorCode.DUPLICATE_GROUP_DOCUMENT);
        }

        // 문서 생성 및 저장
        GroupDocument groupDocument = GroupDocument.create(group, selfStudy);
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

        // 그룹에 연결된 문서 조회
        var documents = groupDocumentRepository.findAllByStudyGroupId(groupId);

        // S3 삭제 대상 수집
        for (GroupDocument doc : documents) {

            SelfStudy selfStudy = doc.getSelfStudy();

            // 다른 그룹에서 사용 중인지 확인
            boolean usedElsewhere =
                    groupDocumentRepository.countBySelfStudy(selfStudy) > 1;

            if (!usedElsewhere) {
                // S3 먼저 삭제
                s3FileManager.delete(selfStudy.getS3Key());
            }
        }

        // DB 삭제 실행
        deleteStudyGroupFromDb(group);
    }

    // DB에서 삭제
    @Transactional
    protected void deleteStudyGroupFromDb(StudyGroup group) {

        groupDocumentRepository.deleteAllByStudyGroup(group);
        groupMemberRepository.deleteAllByStudyGroup(group);
        studyGroupRepository.delete(group);
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

    // 그룹 문서 파일 단건 조회
    public GroupFileResponse getGroupFile(Long groupId, Long groupDocumentId, Long currentUserId) {

        // 멤버 권한 검증
        if (!groupMemberRepository.existsByStudyGroupIdAndUserId(groupId, currentUserId)) {
            throw new GeneralException(ErrorCode.NOT_GROUP_MEMBER);
        }

        // 해당 그룹의 문서인지 함께 검증
        GroupDocument groupDocument = groupDocumentRepository.findByIdAndStudyGroupId(groupDocumentId, groupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // Presigned URL 생성 후 반환
        String presignedUrl = s3FileManager.generatePresignedUrl(groupDocument.getSelfStudy().getS3Key());

        return GroupFileResponse.from(groupDocument, presignedUrl);
    }

}
