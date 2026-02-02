package LDHD.project.domain.auth.repository;

import LDHD.project.domain.auth.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository  extends CrudRepository<RefreshToken, String> {
    // 기본 CRUD 기능 지원
}
