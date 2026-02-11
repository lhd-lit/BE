package LDHD.project.domain.group.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.group.repository.GroupMemberRepository;
import LDHD.project.domain.group.repository.StudyGroupRepository;
import LDHD.project.domain.group.web.dto.GroupDocumentAddRequest;
import LDHD.project.domain.group.web.dto.GroupDocumentAddResponse;
import LDHD.project.domain.group.web.dto.StudyGroupCreateRequest;
import LDHD.project.domain.group.web.dto.StudyGroupCreateResponse;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
}
