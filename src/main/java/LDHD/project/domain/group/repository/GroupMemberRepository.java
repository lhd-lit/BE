package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByStudyGroup_IdAndUser_Id(Long studyGroupId, Long userId);

    Optional<GroupMember> findByStudyGroup_IdAndUser_Id(Long studyGroupId, Long userId);
}
