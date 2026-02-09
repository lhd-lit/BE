package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupDocumentRepository extends JpaRepository<GroupDocument, Long> {

    boolean existsByStudyGroupIdAndSelfStudyId(Long studyGroupId, Long selfStudyId);
}
