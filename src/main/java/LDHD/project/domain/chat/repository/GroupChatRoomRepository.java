package LDHD.project.domain.chat.repository;

import LDHD.project.domain.chat.entity.GroupChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupChatRoomRepository extends JpaRepository<GroupChatRoom, Long> {
    // 스터디 그룹 ID로 채팅방 조회
    Optional<GroupChatRoom> findByStudyGroup_Id(Long studyGroupId);
}
