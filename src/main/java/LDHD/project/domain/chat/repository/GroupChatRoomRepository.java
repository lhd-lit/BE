package LDHD.project.domain.chat.repository;

import LDHD.project.domain.chat.entity.GroupChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupChatRoomRepository extends JpaRepository<GroupChatRoom, Long> {
    // 스터디 그룹 ID로 채팅방 조회
    Optional<GroupChatRoom> findByStudyGroup_Id(Long studyGroupId);

    // 1. 페이징을 위해 ID만 먼저 조회 (커버링 인덱스 효과)
    // 사용자가 속한 그룹의 멤버십을 확인하여 채팅방 ID 목록을 가져옴
    @Query("SELECT r.id FROM GroupChatRoom r " + "WHERE r.studyGroup.id IN (" +
            "   SELECT m.studyGroup.id FROM GroupMember m WHERE m.user.id = :userId" + ")")
    Page<Long> findRoomIdsByUserId(@Param("userId") Long userId, Pageable pageable);
    // 2. 조회된 ID 리스트로 데이터 조회 (Fetch Join 적용)
    // N+1 문제를 방지하기 위해 StudyGroup을 한 번에 가져옴
    @Query("SELECT r FROM GroupChatRoom r " + "JOIN FETCH r.studyGroup " + "WHERE r.id IN :ids")
    List<GroupChatRoom> findAllByIdIn(@Param("ids") List<Long> ids);
}
