package LDHD.project.domain.group.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.group.GroupRole;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "studyGroup")
public class StudyGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_group_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @BatchSize(size = 100) // 그룹 조회 시 그룹 멤버도 포함해 같이 조회
    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL,orphanRemoval = true)
    //그룹 저장, 삭제 시 멤버들도 함께 적용, 리스트에서 멤버 삭제 시 DB에서도 해당 멤버 삭제
    private List<GroupMember> members = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupDocument> documents = new ArrayList<>();

    @Builder
    public StudyGroup(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }
    // 멤버 추가
    public void addMember(User user, GroupRole role) {
        // 이미 존재하는지 체크하는 로직이 도메인 서비스 레벨에 있어야 함
        GroupMember member = GroupMember.create(this, user, role);
        // 그룹멤버 엔티티 생성
        // Cascade로 인해 GroupMember 리스트 에 저장 시 DB에도 저장됨
        this.members.add(member);
    }

    // 생성 로직: 그룹 생성 시 방장은 자동으로 멤버(LEADER)로 추가되어야 함
    public static StudyGroup create(String name, String description, User owner) {
        StudyGroup group = new StudyGroup(name, description, owner);
        group.addMember(owner, GroupRole.LEADER); // 방장 권한 부여
        return group;
    }

}
