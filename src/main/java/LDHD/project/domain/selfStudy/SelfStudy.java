package LDHD.project.domain.selfStudy;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "self_study")
public class SelfStudy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "self_study_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User uploader;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(length = 255)
    private String namespace;

    // fileUrl → s3Key (인프라 URL 대신 S3 객체 경로만 저장)
    // 예: user/12/uuid_document.pdf
    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    //원본 파일명(UI 표시 및 다운로드용)
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    //문서에서 추출된 전체 텍스트(AI용)
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Column(name = "file_size", nullable = true)
    private Long fileSize;

    @Builder
    public SelfStudy(User uploader, String title, String description, String namespace, String s3Key, String originalFileName
            ,String extractedText, Long fileSize) {
        this.uploader = uploader;
        this.title = title;
        this.description = description;
        this.namespace = namespace;
        this.s3Key = s3Key;
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
        this.fileSize = fileSize;
    }

    public static SelfStudy create(User uploader, String title, String description, String s3Key,
                                   String originalFileName, String extractedText, Long fileSize) {
        return SelfStudy.builder()
                .uploader(uploader)
                .title(title)
                .description(description)
                .s3Key(s3Key)
                .originalFileName(originalFileName)
                .extractedText(extractedText)
                .fileSize(fileSize)
                .build();
    }

    // Self-Study 제목, 설명만 변경 가능
    public void update(String title, String description){
        this.title = title;
        this.description = description;
    }

    // 파일 교체
    public void replaceFile(String s3Key, String originalFileName, String extractedText, Long fileSize) {
        this.s3Key = s3Key;
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
        this.fileSize = fileSize;
    }

    // 조회 시 갱신
    public void updateLastViewedAt() {
        this.lastViewedAt = LocalDateTime.now();
    }

    public void updateNamespace(String namespace) {
        this.namespace = namespace;
    }
}
