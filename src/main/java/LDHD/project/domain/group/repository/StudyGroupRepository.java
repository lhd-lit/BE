package LDHD.project.domain.group.repository;

import LDHD.project.domain.group.entity.StudyGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    // 내가 속한 그룹 목록
    @Query("SELECT g FROM StudyGroup g JOIN FETCH g.owner " + "JOIN GroupMember m ON m.studyGroup.id = g.id " +
            "WHERE m.user.id = :userId")
    Page<StudyGroup> findAllByMemberId(@Param("userId") Long userId, Pageable pageable);

    // 최근 조회한 순서로 내가 속한 그룹 목록
    @Query("SELECT g FROM StudyGroup g JOIN FETCH g.owner " + "JOIN GroupMember m ON m.studyGroup.id = g.id " +
            "WHERE m.user.id = :userId AND g.lastViewedAt IS NOT NULL " + "ORDER BY g.lastViewedAt DESC")

    Page<StudyGroup> findAllByMemberIdOrderByLastViewedAt(@Param("userId") Long userId, Pageable pageable);

    Optional<StudyGroup> findTopByMembers_User_IdAndLastViewedAtIsNotNullOrderByLastViewedAtDesc(Long userId);
}
