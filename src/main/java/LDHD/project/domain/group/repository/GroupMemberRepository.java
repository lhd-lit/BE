package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    boolean existsByStudyGroupIdAndUserId(Long studyGroupId, Long userId);

    @Query("SELECT m.studyGroup.id, count(m) " +
            "FROM GroupMember m " +
            "WHERE m.studyGroup.id IN :groupIds " +
            "GROUP BY m.studyGroup.id")
    List<Object[]> countMembersByStudyGroupIdsIn(@Param("groupIds") List<Long> groupIds);

    // 이미 멤버인 사용자 조회 (초대 시 중복 방지)
    List<GroupMember> findAllByStudyGroup_IdAndUser_IdIn(Long studyGroupId, List<Long> userIds);

    Optional<GroupMember> findByStudyGroup_IdAndUser_Id(Long studyGroupId, Long userId);

    int countByStudyGroup_Id(Long studyGroupId);
}
