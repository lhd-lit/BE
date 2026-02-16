package LDHD.project.domain.chat.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.group.entity.StudyGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_chat_room")
public class GroupChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_chat_room_id")
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Builder
    private GroupChatRoom(StudyGroup studyGroup){
        this.studyGroup = studyGroup;
    }

    // 그룹 채팅방 생성
    public static GroupChatRoom create(StudyGroup studyGroup){
        return GroupChatRoom.builder()
                .studyGroup(studyGroup)
                .build();
    }
}
