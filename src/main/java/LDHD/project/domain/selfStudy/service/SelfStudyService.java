package LDHD.project.domain.selfStudy.service;

import LDHD.project.common.aws.S3FileUploader;
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
@Transactional
public class SelfStudyService {

    private final SelfStudyRepository selfStudyRepository;
    private final UserRepository userRepository;
    private final S3FileUploader s3FileUploader;
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

        //3. S3 파일 업로드 URL
        String fileUrl = s3FileUploader.upload(file);

        //4. 파일 텍스트 추출(AI 학습용)
        String extractedText = fileTextParser.extractText(file);

        SelfStudy selfStudy = SelfStudy.create(
                user, request.getTitle(),
                request.getDescription(),
                fileUrl, file.getOriginalFilename(),
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
        if (selfStudy.getFileUrl() != null && !selfStudy.getFileUrl().isEmpty()) {
            s3FileUploader.deleteFile(selfStudy.getFileUrl());
        }
        // PostgresSql DB에서 삭제
        selfStudyRepository.delete(selfStudy);

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
    @Transactional
    public Page<GetSelfStudyListResponse> getAllSelfStudyList(int page, int size) {
        // 페이지는 0부터 시작, 작성 시간 기준으로 내림차순 정리
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<SelfStudy> selfStudies = selfStudyRepository.findAll(pageable);

        return selfStudies.map(GetSelfStudyListResponse::from);
    }
    // 2. 사용자용
    public Page<GetSelfStudyListResponse> getMySelfStudyList(Long userId, int page, int size) {
        // 페이지는 0부터 시작, 작성 시간 기준으로 내림차순 정리
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<SelfStudy> selfStudies = selfStudyRepository.findAllByUploader_Id(userId, pageable);

        return selfStudies.map(GetSelfStudyListResponse::from);
    }
}
