package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupDocumentRepository extends JpaRepository<GroupDocument, Long> {

    // "selfStudy"와 "selfStudy.user"를 같이 가져오라고(FETCH JOIN) 지시
    @EntityGraph(attributePaths = {"uploader"})
    Page<GroupDocument> findAllByStudyGroupId(Long studyGroupId, Pageable pageable);

    Optional<GroupDocument> findByIdAndStudyGroupId(Long groupDocumentId, Long groupId);

    List<GroupDocument> findAllByStudyGroupId(Long groupId);

    void deleteAllByStudyGroup(StudyGroup group);

    List<GroupDocument> findAllByStudyGroupIdAndUploaderId(Long studyGroupId, Long uploaderId);

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM GroupDocument d WHERE d.uploader.id = :userId")
    Long sumFileSizeByUploaderId(@Param("userId") Long userId);

}
