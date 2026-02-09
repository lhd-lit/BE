package LDHD.project.domain.group.entity;

import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.selfStudy.SelfStudy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "group_document")
public class GroupDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_document_id")
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup; // 어느 그룹 문서인지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "self_study_id", nullable = false)
    private SelfStudy selfStudy; // 실제 문서 정보

    @Builder
    public GroupDocument(StudyGroup studyGroup, SelfStudy selfStudy) {
        this.studyGroup = studyGroup;
        this.selfStudy = selfStudy;
    }

    public static GroupDocument create(StudyGroup studyGroup, SelfStudy selfStudy) {
        return new GroupDocument(studyGroup, selfStudy);
    }
}
