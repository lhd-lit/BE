package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupDocument;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.selfStudy.SelfStudy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupDocumentRepository extends JpaRepository<GroupDocument, Long> {

    boolean existsByStudyGroupAndSelfStudy(StudyGroup studyGroup, SelfStudy selfStudy);
    void deleteBySelfStudy(SelfStudy selfStudy);
}
