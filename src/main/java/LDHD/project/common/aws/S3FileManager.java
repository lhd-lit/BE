package LDHD.project.common.aws;

import LDHD.project.common.aws.web.dto.PresignedUploadResponse;
import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileManager {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    // 학습 문서(파일)를 S3에 업로드 후 key로 반환
    public String upload(MultipartFile file, Long userId) {

        // 파일 유효성 검사(존재, 용량, 타입)
        validateFile(file);

        // 파일명에서 특수문자 제거(공격 방지)
        String sanitizedName = sanitizeFileName(file.getOriginalFilename());
        // 유저별 폴더 구조 생성 및 UUID를 더해 파일명 중복을 방지합니다. (예: user/1/uuid_filename.pdf)
        String key = "user/" + userId + "/" + UUID.randomUUID() + "_" + sanitizedName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            // S3로 데이터 전송
            s3Client.putObject(request,RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("S3 파일 업로드 완료 - key: {}", key);
            return key; // ✅ URL이 아닌 key만 반환

        } catch (Exception e) {
            log.error("S3 파일 업로드 실패 - userId: {}", userId, e);
            throw new GeneralException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
    // fileUrl로 Presigned URL 생성 (파일 조회 시 호출)
    // DB에는 일반 S3 key 저장 → 조회 시에만 Presigned URL 반환(15분간 유효)
    public String generatePresignedUrl(String key) {
        try {
            // 조회 요청 객체 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15)) // 유효시간 15분 설정
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest)
                    .url()
                    .toString();

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패 - key: {}", key, e);
            throw new GeneralException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    // key 기반 파일 삭제
    public void delete(String key) {
        try {
            // 삭제 요청 객체 생성
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            log.info("S3 파일 삭제 완료 - key: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패 - key: {}", key, e);
            throw new GeneralException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    // 파일 유효성 검증
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > 50L * 1024 * 1024) { // 파일 크기가 50MB 초과하는지
            throw new GeneralException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        // pdf, docx 파일만 허용 그 외 : 에러 코드 반환
        String contentType = file.getContentType();
        boolean isAllowedType = "application/pdf".equals(contentType) ||
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);

        if (!isAllowedType) {
            throw new GeneralException(ErrorCode.INVALID_FILE_TYPE);
        }
    }
    // 경로 조작(특수문자) 공격 방지
    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()
                || originalFileName.contains("..")) {
            throw new GeneralException(ErrorCode.INVALID_FILE_NAME);
        }
        return originalFileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    // 클라이언트가 S3 에 직접 업로드할 때 사용
    public PresignedUploadResponse generatePresignedUploadUrl(String originalFileName, Long userId) {
        // 파일명 특수 문자 제거
        String sanitizedName = sanitizeFileName(originalFileName);

        // S3 Key 생성
        String s3Key = "user/" + userId + "/" + UUID.randomUUID() + "_" + sanitizedName;

        try{
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10)) // 10분간 유효
                    .putObjectRequest(putObjectRequest)
                    .build();

            String presignedUrl = s3Presigner.presignPutObject(presignRequest)
                    .url()
                    .toString();

            log.info("PUT Presigned URL 생성 완료 - s3Key: {}", s3Key);

            return PresignedUploadResponse.of(presignedUrl, s3Key, originalFileName);

        }catch (Exception e) {
            log.error("PUT Presigned URL 생성 실패 - userId: {}", userId, e);
            throw new GeneralException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}

