package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByStudyGroupIdAndUserId(Long studyGroupId, Long userId);

    Optional<GroupMember> findByStudyGroupIdAndUserId(Long studyGroupId, Long userId);
}
