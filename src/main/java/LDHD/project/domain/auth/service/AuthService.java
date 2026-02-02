package LDHD.project.domain.auth.service;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.security.jwt.JwtTokenProvider;
import LDHD.project.domain.auth.RefreshToken;
import LDHD.project.domain.auth.web.controller.dto.TokenResponse;
import LDHD.project.domain.user.User;
import LDHD.project.domain.auth.repository.RefreshTokenRepository;
import LDHD.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-token-expiration-millis}")
    private long refreshTokenExpirationMillis;

    @Transactional
    // 토큰 재발급 로직
    public TokenResponse reissueAccessToken(String refreshToken){

        // Refresh Token 유효성 검증
        if(!jwtTokenProvider.validateToken(refreshToken)){
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }
        // 토큰 타입 검증
        jwtTokenProvider.validateTokenType(refreshToken, "REFRESH");

        // Refresh Token에서 userId 추출
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // Redis에 저장된 것 중 userId로 저장된 토큰 찾기
        RefreshToken savedToken = refreshTokenRepository.findById(String.valueOf(userId))
                .orElseThrow(() -> new GeneralException(ErrorCode.INVALID_TOKEN));

        if (!savedToken.getRefreshToken().equals(refreshToken)) {
            // 토큰 불일치 시 탈취로 간주 -> 토큰 삭제 (강제 로그아웃)
            refreshTokenRepository.delete(savedToken);
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }
        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 새로운 Access 와 Refresh 토큰 재발급(RTR)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRoleKey());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 새로운 Refresh 토큰으로 Redis 갱신 (기존 ID가 같으므로 덮어쓰기됨)
        refreshTokenRepository.save(new RefreshToken(String.valueOf(userId), newRefreshToken));

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .refreshTokenMaxAge(refreshTokenExpirationMillis / 1000)
                .build();
    }

    @Transactional
    public void logout(String refreshToken){

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        refreshTokenRepository.findById(String.valueOf(userId))
                .ifPresent(refreshTokenRepository::delete);
    }
}
