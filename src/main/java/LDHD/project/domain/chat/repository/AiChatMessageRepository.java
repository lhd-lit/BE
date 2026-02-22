package LDHD.project.domain.chat.repository;

import LDHD.project.domain.chat.entity.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findByChatRoom_IdOrderByIdAsc(Long chatRoomId);

    // 최신 메시지 조회
    Slice<AiChatMessage> findByChatRoom_IdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    // 이전 메시지 조회(커서 페이징)
    Slice<AiChatMessage> findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(Long chatRoomId,LocalDateTime cursor,
            Pageable pageable
    );
}
