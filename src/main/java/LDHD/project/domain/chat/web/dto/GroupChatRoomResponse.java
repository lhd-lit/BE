package LDHD.project.domain.chat.web.dto;

import LDHD.project.domain.chat.entity.GroupChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "그룹 채팅방 정보 응답 DTO")
public class GroupChatRoomResponse {

    @Schema(description = "채팅방 ID")
    private Long chatRoomId;

    @Schema(description = "연결된 스터디 그룹 ID")
    private Long studyGroupId;

    @Schema(description = "채팅방 이름 (스터디 그룹명)")
    private String roomName;

    @Schema(description = "현재 참여 인원 수")
    private Integer memberCount;

    public static GroupChatRoomResponse of(GroupChatRoom room, int memberCount) {
        return GroupChatRoomResponse.builder()
                .chatRoomId(room.getId())
                .studyGroupId(room.getStudyGroup().getId())
                .roomName(room.getStudyGroup().getName())
                .memberCount(memberCount)
                .build();
    }
}
