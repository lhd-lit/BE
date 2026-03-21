package LDHD.project.domain.group.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "document_comment")
public class DocumentComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_document_id", nullable = false)
    private GroupDocument groupDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // 선택한 텍스트 원문 (루트 댓글만 존재)
    @Column(name = "highlighted_text", columnDefinition = "TEXT")
    private String highlightedText;

    // 텍스트 선택 시작 위치 (루트 댓글만 존재, 프론트 Selection API 기준)
    @Column(name = "anchor_offset")
    private Integer anchorOffset;

    // 텍스트 선택 끝 위치 (루트 댓글만 존재)
    @Column(name = "focus_offset")
    private Integer focusOffset;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 대댓글 구조 - null이면 루트 댓글, 값 있으면 대댓글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private DocumentComment parentComment;

    // 자식 댓글(대댓글) 목록 - 1 depth만 허용
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<DocumentComment> replies = new ArrayList<>();

    @Builder
    private DocumentComment(GroupDocument groupDocument, User author, String highlightedText, Integer anchorOffset,
                            Integer focusOffset, String content,  DocumentComment parentComment) {
        this.groupDocument = groupDocument;
        this.author = author;
        this.highlightedText = highlightedText;
        this.anchorOffset = anchorOffset;
        this.focusOffset = focusOffset;
        this.content = content;
        this.parentComment = parentComment;
    }

    // 루트 댓글 생성 (하이라이트 + 첫 댓글)
    public static DocumentComment createRoot(GroupDocument document, User author,
                                             String highlightedText, Integer anchorOffset,
                                             Integer focusOffset, String content) {
        return DocumentComment.builder()
                .groupDocument(document)
                .author(author)
                .highlightedText(highlightedText)
                .anchorOffset(anchorOffset)
                .focusOffset(focusOffset)
                .content(content)
                .parentComment(null)
                .build();
    }

    // 대댓글 생성
    public static DocumentComment createReply(GroupDocument document, User author,
                                              String content, DocumentComment parentComment) {
        return DocumentComment.builder()
                .groupDocument(document)
                .author(author)
                .content(content)
                .parentComment(parentComment)
                .build();
    }

    // 댓글 내용 수정
    public void updateContent(String content) {
        this.content = content;
    }
}
