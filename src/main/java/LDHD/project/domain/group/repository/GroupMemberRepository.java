package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByStudyGroupIdAndUserId(Long studyGroupId, Long userId);

    @Query("SELECT gm.user.id FROM GroupMember gm WHERE gm.studyGroup.id = :studyGroupId")
    List<Object[]> countMembersByStudyGroupIdsIn(@Param("groupIds") List<Long> groupIds);

    // 이미 멤버인 사용자 조회 (초대 시 중복 방지)
    List<GroupMember> findAllByStudyGroup_IdAndUser_IdIn(Long studyGroupId, List<Long> userIds);

    Optional<GroupMember> findByStudyGroup_IdAndUser_Id(Long studyGroupId, Long userId);

    List<GroupMember> findAllByStudyGroup_Id(Long studyGroupId);

    int countByStudyGroup_Id(Long studyGroupId);
}
