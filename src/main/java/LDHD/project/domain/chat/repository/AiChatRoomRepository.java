package LDHD.project.domain.chat.repository;

import LDHD.project.domain.chat.entity.AiChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatRoomRepository extends JpaRepository<AiChatRoom, Long> {
    List<AiChatRoom> findByUser_Id(Long userId);
}
