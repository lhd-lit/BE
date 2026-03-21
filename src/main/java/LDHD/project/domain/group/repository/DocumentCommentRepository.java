package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {

    // 문서의 루트 댓글 + 대댓글 + 작성자 한 번에 조회 (N+1 방지)
    @Query("SELECT DISTINCT c FROM DocumentComment c " + "LEFT JOIN FETCH c.replies r " + "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH r.author " + "WHERE c.groupDocument.id = :documentId " + "AND c.parentComment IS NULL " +
            "ORDER BY c.createdAt ASC")

    List<DocumentComment> findRootCommentsByDocumentId(@Param("documentId") Long documentId);

    // 댓글 단건 조회 + 작성자 fetch (삭제/수정 권한 확인용)
    @Query("SELECT c FROM DocumentComment c " + "JOIN FETCH c.author " + "JOIN FETCH c.groupDocument gd " +
            "JOIN FETCH gd.studyGroup " + "WHERE c.id = :commentId")

    Optional<DocumentComment> findByIdWithAuthorAndGroup(@Param("commentId") Long commentId);

    // 부모 댓글 조회 + 문서 정보 fetch (대댓글 생성 시 부모 depth 확인용)
    @Query("SELECT c FROM DocumentComment c " + "LEFT JOIN FETCH c.parentComment " + "WHERE c.id = :commentId")
    Optional<DocumentComment> findByIdWithParent(@Param("commentId") Long commentId);
}
