package LDHD.project.domain.bookmark;


import LDHD.project.common.entity.BaseEntity;
import LDHD.project.domain.group.entity.StudyGroup;
import LDHD.project.domain.selfStudy.SelfStudy;
import LDHD.project.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Bookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selfStudy_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    SelfStudy selfStudy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studyGroup_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    StudyGroup studyGroup;

    public Bookmark(User user, SelfStudy selfStudy) {
        this.user = user;
        this.selfStudy = selfStudy;
    }


}
