package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.selfStudy.SelfStudy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupDocumentRepository extends JpaRepository<GroupDocument, Long> {

    boolean existsByStudyGroupAndSelfStudy(StudyGroup studyGroup, SelfStudy selfStudy);
    void deleteBySelfStudy(SelfStudy selfStudy);

    // "selfStudy"와 "selfStudy.user"를 같이 가져오라고(FETCH JOIN) 지시
    @EntityGraph(attributePaths = {"selfStudy", "selfStudy.user"})
    Page<GroupDocument> findAllByStudyGroupId(Long studyGroupId, Pageable pageable);

    @Query("SELECT gd FROM GroupDocument gd " + "JOIN FETCH gd.selfStudy s " + "JOIN FETCH s.uploader " +
            "WHERE gd.id = :groupDocumentId " + "AND gd.studyGroup.id = :groupId")
    Optional<GroupDocument> findByIdAndStudyGroupId(@Param("groupDocumentId") Long groupDocumentId,@Param("groupId") Long groupId);

    List<GroupDocument> findAllByStudyGroupId(Long groupId);

    long countBySelfStudy(SelfStudy selfStudy);

    void deleteAllByStudyGroup(StudyGroup group);


}
