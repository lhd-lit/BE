package LDHD.project.domain.selfStudy.service;

import LDHD.project.common.aws.S3FileManager;
import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.utils.FileTextParser;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.selfStudy.web.controller.dto.*;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelfStudyService {

    private final SelfStudyRepository selfStudyRepository;
    private final UserRepository userRepository;
    private final S3FileManager s3FileManager;
    private final FileTextParser fileTextParser;
    private final GroupDocumentRepository groupDocumentRepository;
    // self-study 생성 로직
    @Transactional
    public CreateSelfStudyResponse createSelfStudy( Long currentUserId, CreateSelfStudyRequest request, MultipartFile file) {

        //1. 사용자 존재 확인
        User user = userRepository.findById(currentUserId).orElseThrow(
                ()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        //2. 파일 유효성 검사
        if(file.isEmpty()){throw new GeneralException(ErrorCode.VALIDATION_FAILED);}

        // userId 전달 → user/{userId}/uuid_file.pdf 경로로 저장 & key만 반환 (URL 아님)
        String s3Key = s3FileManager.upload(file, currentUserId);

        //4. 파일 텍스트 추출(AI 학습용)
        String extractedText = fileTextParser.extractText(file);

        SelfStudy selfStudy = SelfStudy.create(
                user, request.getTitle(),
                request.getDescription(),
                s3Key, file.getOriginalFilename(),
                extractedText
        );

        selfStudyRepository.save(selfStudy);

        return new CreateSelfStudyResponse(
                selfStudy.getId(),
                selfStudy.getTitle(),
                selfStudy.getDescription()
        );
    }

    // self-study 삭제 로직
    @Transactional
    public void deleteSelfStudy(Long selfStudyId, Long currentUserId) {

        // 1. 게시물 존재 확인 (POST_NOT_FOUND)
        SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 2. 권한 확인 (UNAUTHORIZED - 작성자와 요청자가 다를 경우)
        if (!selfStudy.getUploader().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }
        groupDocumentRepository.deleteBySelfStudy(selfStudy);

        // 3. S3 저장소에 있는 파일 삭제
        s3FileManager.delete(selfStudy.getS3Key());
        // PostgresSql DB에서 삭제
        deleteSelfStudyFromDb(selfStudy);



    }

    // self-study 수정 로직
    @Transactional
    public UpdateSelfStudyResponse updateSelfStudy(Long selfStudyId, UpdateSelfStudyRequest request,
                                                           Long currentUserId) {
        // 1. 게시물 존재 확인 (POST_NOT_FOUND)
        SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 2. 권한 확인 (UNAUTHORIZED - 작성자와 요청자가 다를 경우)
        if (!selfStudy.getUploader().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        selfStudy.update(request.getTitle(), request.getDescription());

        return new UpdateSelfStudyResponse(
                selfStudy.getId(),
                selfStudy.getTitle(),
                selfStudy.getDescription()
        );
    }
    // self-study 목록 조회(1.관리자용-all 2.사용자용-me)
    // 1. 관리자용(전체 조회)
    @Transactional(readOnly = true)
    public Page<GetSelfStudyListResponse> getAllSelfStudyList(int page, int size) {
        // 페이지는 0부터 시작, 작성 시간 기준으로 내림차순 정리
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<SelfStudy> selfStudies = selfStudyRepository.findAll(pageable);

        return selfStudies.map(GetSelfStudyListResponse::from);
    }
    // 2. 사용자용
    @Transactional(readOnly = true)
    public Page<GetSelfStudyListResponse> getMySelfStudyList(Long userId, int page, int size) {
        // 페이지는 0부터 시작, 작성 시간 기준으로 내림차순 정리
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<SelfStudy> selfStudies = selfStudyRepository.findAllByUser_Id(userId, pageable);

        return selfStudies.map(GetSelfStudyListResponse::from);
    }
    // SelfStudy 단건 조회 + lastViewedAt 갱신
    @Transactional
    public GetSelfStudyListResponse getSelfStudy(Long selfStudyId, Long currentUserId) {

        SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        if (!selfStudy.getUploader().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        // 조회 시 마지막 조회 시간 갱신
        selfStudy.updateLastViewedAt();
        return GetSelfStudyListResponse.from(selfStudy);
    }
    // 최근 조회한 순서로 목록 반환
    public Page<GetSelfStudyListResponse> getRecentViewedList(Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return selfStudyRepository.findAllByUserIdOrderByLastViewedAt(userId, pageable)
                .map(GetSelfStudyListResponse::from);
    }

    // SelfStudy 파일(학습 문서) 조회
    public SelfStudyFileResponse getSelfStudyFile(Long selfStudyId, Long currentUserId) {

        SelfStudy selfStudy = selfStudyRepository.findById(selfStudyId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));

        // 본인 파일만 조회 가능 => 본인 검증
        if(!selfStudy.getUploader().getId().equals(currentUserId)){
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }

        // 1. S3Uploader를 통해 15분짜리 Presigned URL 즉시 생성
        String presignedUrl = s3FileManager.generatePresignedUrl(selfStudy.getS3Key());

        // 2. 엔티티 데이터와 생성된 Presigned URL을 함께 DTO에 담아 반환
        return SelfStudyFileResponse.of(selfStudy, presignedUrl);
    }

    // DB 삭제 전용
    @Transactional
    protected void deleteSelfStudyFromDb(SelfStudy selfStudy) {

        // 그룹 문서 연결 제거
        groupDocumentRepository.deleteBySelfStudy(selfStudy);

        // SelfStudy 삭제
        selfStudyRepository.delete(selfStudy);
    }
}
