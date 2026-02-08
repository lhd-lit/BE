package LDHD.project.domain.group.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.group.GroupRole;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"study_group_id", "user_id"})
})
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_member_Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Builder
    public GroupMember(StudyGroup studyGroup, User user, GroupRole role) {
        this.studyGroup = studyGroup;
        this.user = user;
        this.role = role;
    }

    public static GroupMember create(StudyGroup studyGroup, User user, GroupRole role) {
        return GroupMember.builder()
                .studyGroup(studyGroup)
                .user(user)
                .role(role)
                .build();
    }
}
