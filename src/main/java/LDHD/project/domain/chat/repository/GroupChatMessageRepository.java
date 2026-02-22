package LDHD.project.domain.chat.repository;

import LDHD.project.domain.chat.entity.GroupChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Long> {

    // 메시지를 가져올 때 Sender(User) 정보를 한방 쿼리(Fetch Join)로 가져옴
    @Query("SELECT m FROM GroupChatMessage m JOIN FETCH m.sender WHERE m.chatRoom.id = :chatRoomId ORDER BY m.createdAt ASC")
    List<GroupChatMessage> findAllByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    // 신규 메시지 조회
    @EntityGraph(attributePaths = {"sender"})
    Slice<GroupChatMessage> findByChatRoom_IdOrderByCreatedAtDesc( Long chatRoomId, Pageable pageable);

    // 이전 메시지 조회(커서 페이징)
    @EntityGraph(attributePaths = {"sender"})
    Slice<GroupChatMessage> findByChatRoom_IdAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long chatRoomId,
            LocalDateTime cursor,
            Pageable pageable
    );

    // 읽지 않은 메시지 개수
    @Query("SELECT COUNT(m) FROM GroupChatMessage m " + "WHERE m.chatRoom.id = :chatRoomId " +
            "AND m.createdAt > :lastReadTime")

    Long countNewMessages(@Param("chatRoomId") Long chatRoomId, @Param("lastReadTime") LocalDateTime lastReadTime);
}
