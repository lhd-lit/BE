package LDHD.project.domain.group.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_document")
public class GroupDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_document_id")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup; // 어느 그룹 문서인지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;


    @Builder
    private GroupDocument(StudyGroup studyGroup, User uploader, String title, String description, String s3Key,
                          String originalFileName, String extractedText) {

        this.studyGroup = studyGroup;
        this.uploader = uploader;
        this.title = title;
        this.description = description;
        this.s3Key = s3Key;
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
    }

    public static GroupDocument create(StudyGroup studyGroup, User uploader, String title, String description,
                                       String s3Key, String originalFileName, String extractedText) {

        return GroupDocument.builder()
                .studyGroup(studyGroup)
                .uploader(uploader)
                .title(title)
                .description(description)
                .s3Key(s3Key)
                .originalFileName(originalFileName)
                .extractedText(extractedText)
                .build();
    }

    public void updateLastViewedAt() {
        this.lastViewedAt = LocalDateTime.now();
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
