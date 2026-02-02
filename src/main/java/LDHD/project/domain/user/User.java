package LDHD.project.domain.user;

import LDHD.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@NoArgsConstructor
@Getter
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // profile 조회용
    @Column
    private Integer gender;

    @Column
    private String birthDate;

    // 권환 확인
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // provider & providerId : 현재는 필요 x, 나중에 카카오나 네이버 로그인까지 확장 시 사용자 식별에 사용
    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    @Builder
    public User(String name, String email, Integer gender, String birthDate, Role role, String provider, String providerId) {

        this.name = name;
        this.email = email;
        this.gender = gender;
        this.birthDate = birthDate;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    public User update(String name){
        this.name = name;
        return this;
    }
    public String getRoleKey() {
        return this.role.getKey();
    }
}
