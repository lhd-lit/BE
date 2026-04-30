package LDHD.project.domain.selfStudy.service;

import LDHD.project.common.aws.S3FileManager;
import LDHD.project.common.aws.web.dto.PresignedUploadResponse;
import LDHD.project.common.aws.web.dto.SelfStudyConfirmRequest;
import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.utils.FileTextParser;
import LDHD.project.domain.chat.client.AiClient;
import LDHD.project.domain.group.repository.GroupDocumentRepository;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.selfStudy.repository.SelfStudyRepository;
import LDHD.project.domain.selfStudy.web.controller.dto.*;
import LDHD.project.domain.user.User;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelfStudyService {

    private final SelfStudyRepository selfStudyRepository;
    private final UserRepository userRepository;
    private final S3FileManager s3FileManager;
    private final FileTextParser fileTextParser;
    private final GroupDocumentRepository groupDocumentRepository;
    private final AiClient aiClient;

   /*
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
                extractedText,
                file.getSize()
        );

        selfStudyRepository.save(selfStudy);

        return new CreateSelfStudyResponse(
                selfStudy.getId(),
                selfStudy.getTitle(),
                selfStudy.getDescription()
        );
    }
    */

    // self-study 삭제 로직
    @Transactional
    public void deleteSelfStudy(Long selfStudyId, Long currentUserId) {

        SelfStudy selfStudy = findSelfStudyOrThrow(selfStudyId);
        validateOwner(selfStudy, currentUserId);

        // AI 서버 세션 초기화 (해당 SelfStudy와 관련된 모든 세션)
        // namespace 패턴으로 세션을 찾아 초기화하려면 별도 로직 필요
        // 현재는 생략 (필요시 구현)

        // S3 파일 삭제
        s3FileManager.delete(selfStudy.getS3Key());

        // DB 삭제
        deleteSelfStudyFromDb(selfStudy);
    }

    // self-study 수정 로직
    @Transactional
    public UpdateSelfStudyResponse updateSelfStudy(Long selfStudyId,
                                                   UpdateSelfStudyRequest request,
                                                   Long currentUserId) {

        SelfStudy selfStudy = findSelfStudyOrThrow(selfStudyId);
        validateOwner(selfStudy, currentUserId);

        selfStudy.update(request.getTitle(), request.getDescription());

        return new UpdateSelfStudyResponse(
                selfStudy.getId(),
                selfStudy.getTitle(),
                selfStudy.getDescription()
        );
    }

    // 파일 교체 : update와 분리한 이유 -> 통합했을 시 title만 바꾸고 싶어도 file 교체 로직이 적용됨(단일 책임)
    @Transactional
    public SelfStudyFileResponse replaceSelfStudyFile(Long selfStudyId,
                                                      Long currentUserId,
                                                      MultipartFile file) {

        SelfStudy selfStudy = findSelfStudyOrThrow(selfStudyId);
        validateOwner(selfStudy, currentUserId);

        if (file.isEmpty()) {
            throw new GeneralException(ErrorCode.VALIDATION_FAILED);
        }

        // 기존 파일 삭제
        s3FileManager.delete(selfStudy.getS3Key());

        // 새 파일 업로드
        String newS3Key = s3FileManager.upload(file, currentUserId);
        String newExtractedText = fileTextParser.extractText(file);

        // 엔티티 업데이트
        selfStudy.replaceFile(newS3Key,
                file.getOriginalFilename(),
                newExtractedText,
                file.getSize());

        String presignedUrl = s3FileManager.generatePresignedUrl(newS3Key);
        return SelfStudyFileResponse.of(selfStudy, presignedUrl);
    }

    // 관리자용 전체 조회
    public Page<GetSelfStudyListResponse> getAllSelfStudyList(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return selfStudyRepository.findAll(pageable)
                .map(GetSelfStudyListResponse::from);
    }

    // 사용자용 조회
    public Page<GetSelfStudyListResponse> getMySelfStudyList(Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return selfStudyRepository.findAllByUser_Id(userId, pageable)
                .map(GetSelfStudyListResponse::from);
    }

    /*
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

    // 최근 조회한 순서
    public Page<GetSelfStudyListResponse> getRecentViewedList(Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return selfStudyRepository.findAllByUserIdOrderByLastViewedAt(userId, pageable)
                .map(GetSelfStudyListResponse::from);
    }
    */

    // 최근 조회된 문서 1건
    public GetSelfStudyListResponse getLatestViewedSelfStudy(Long userId) {

        return selfStudyRepository
                .findTopByUploader_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(userId)
                .map(GetSelfStudyListResponse::from)
                .orElse(null);
    }

    // 파일 조회 (lastViewedAt 갱신)
    @Transactional
    public SelfStudyFileResponse getSelfStudyFile(Long selfStudyId, Long currentUserId) {

        SelfStudy selfStudy = findSelfStudyOrThrow(selfStudyId);
        validateOwner(selfStudy, currentUserId);

        selfStudy.updateLastViewedAt();

        String presignedUrl = s3FileManager.generatePresignedUrl(selfStudy.getS3Key());

        return SelfStudyFileResponse.of(selfStudy, presignedUrl);
    }

    // Presigned URL 발급
    public PresignedUploadResponse getPresignedUploadUrl(Long currentUserId, String originalFileName) {

        if (!userRepository.existsById(currentUserId)) {
            throw new GeneralException(ErrorCode.USER_NOT_FOUND);
        }

        return s3FileManager.generatePresignedUploadUrl(originalFileName, currentUserId);
    }

    // 업로드 완료 후 DB 저장
    @Transactional
    public CreateSelfStudyResponse confirmSelfStudy(Long currentUserId,
                                                    SelfStudyConfirmRequest request) {

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        SelfStudy selfStudy = SelfStudy.create(
                user,
                request.getTitle(),
                request.getDescription(),
                request.getS3Key(),
                request.getOriginalFileName(),
                "", // extractedText → AI 서버에서 처리
                request.getFileSize()
        );

        selfStudyRepository.save(selfStudy);

        // namespace 설정
        String namespace = currentUserId + "_" + selfStudy.getId();
        selfStudy.updateNamespace(namespace);
        // 트랜잭션 내에서 dirty checking 보장
        selfStudyRepository.save(selfStudy);

        // S3 Presigned URL(GET)로 파일 다운로드 후 AI 서버 업로드
        try {
            // 기존 s3FileManager.generatePresignedUrl() 재사용
            String getPresignedUrl = s3FileManager.generatePresignedUrl(request.getS3Key());

            // S3에서 파일 다운로드
            byte[] fileBytes = downloadFromUrl(getPresignedUrl);

            // AI 서버에 업로드 (Pinecone 임베딩)
            aiClient.uploadPdf(fileBytes, request.getOriginalFileName(), namespace);

            log.info("AI 서버 PDF 업로드 완료 - selfStudyId: {}, namespace: {}",
                    selfStudy.getId(), namespace);

        } catch (Exception e) {
            // AI 업로드 실패해도 SelfStudy 저장은 유지
            log.error("AI 서버 업로드 실패 (SelfStudy 저장은 유지) - selfStudyId: {}",
                    selfStudy.getId(), e);
        }

        return new CreateSelfStudyResponse(
                selfStudy.getId(),
                selfStudy.getTitle(),
                selfStudy.getDescription()
        );
    }
    // URL에서 파일 바이트 다운로드 (WebClient 재사용)
    private byte[] downloadFromUrl(String url) {
        try {
            return WebClient.create()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("파일 다운로드 실패 - url: {}", url, e);
            throw new RuntimeException("S3 파일 다운로드 실패", e);
        }
    }

    // 공통 메서드 (중복 제거)
    private SelfStudy findSelfStudyOrThrow(Long selfStudyId) {
        return selfStudyRepository.findById(selfStudyId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateOwner(SelfStudy selfStudy, Long currentUserId) {
        if (!selfStudy.getUploader().getId().equals(currentUserId)) {
            throw new GeneralException(ErrorCode.UNAUTHORIZED);
        }
    }

    // DB 삭제 전용
    @Transactional
    protected void deleteSelfStudyFromDb(SelfStudy selfStudy) {
        selfStudyRepository.delete(selfStudy);
    }
}