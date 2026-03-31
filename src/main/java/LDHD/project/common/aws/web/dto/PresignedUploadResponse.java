package LDHD.project.common.aws.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUploadResponse {

    private String presignedUrl; // FE가 S3에 직접 업로드할 URL
    private String s3Key; // 업로드 완료후 DB에 저장할 때 사용
    private String originalFileName; // 원본 파일명

    public static PresignedUploadResponse of(String presignedUrl,
                                             String s3Key,
                                             String originalFileName) {
        return PresignedUploadResponse.builder()
                .presignedUrl(presignedUrl)
                .s3Key(s3Key)
                .originalFileName(originalFileName)
                .build();
    }
}
