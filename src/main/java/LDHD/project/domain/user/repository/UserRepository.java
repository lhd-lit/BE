package LDHD.project.domain.user.repository;

import LDHD.project.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String username);
    Boolean existsByEmail(String email);
    List<User> findAllByIdIn(List<Long> ids);
    List<User> findByEmailStartingWith(String email);

    /** 이메일 @ 앞(로컬 파트)만 대상으로 부분 일치(포함) 검색 — PostgreSQL */
    @Query(value = "SELECT * FROM users u WHERE strpos(lower(split_part(u.email, '@', 1)), lower(:q)) > 0 ORDER BY u.name ASC",
            nativeQuery = true)
    List<User> findByEmailLocalPartContaining(@Param("q") String q);
}
