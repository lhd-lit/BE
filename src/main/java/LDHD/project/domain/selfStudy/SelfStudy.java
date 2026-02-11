package LDHD.project.domain.selfStudy;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

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

    //S3 파일 URL
    @Column(name = "file_Url", nullable = false)
    private String fileUrl;

    //원본 파일명(UI 표시 및 다운로드용)
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    //문서에서 추출된 전체 텍스트(AI용)
    @Lob // DB에 긴 글 저장 가능하도록
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Builder
    public SelfStudy(User uploader, String title, String description, String fileUrl, String originalFileName ,String extractedText) {
        this.uploader = uploader;
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
    }

    public static SelfStudy create(User uploader, String title, String description, String fileUrl, String originalFileName, String extractedText) {
        return SelfStudy.builder()
                .uploader(uploader)
                .title(title)
                .description(description)
                .fileUrl(fileUrl)
                .originalFileName(originalFileName)
                .extractedText(extractedText)
                .build();
    }

    // Self-Study 제목, 설명만 변경 가능
    public void update(String title, String description){
        this.title = title;
        this.description = description;
    }

}
