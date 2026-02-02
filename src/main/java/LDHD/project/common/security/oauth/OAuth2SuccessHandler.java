package LDHD.project.common.security.oauth;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.security.jwt.JwtTokenProvider;
import LDHD.project.common.utils.CookieUtil;
import LDHD.project.domain.auth.RefreshToken;
import LDHD.project.domain.auth.repository.RefreshTokenRepository;
import LDHD.project.domain.user.repository.UserRepository;
import LDHD.project.domain.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final CookieUtil cookieUtil;

    @Value("${jwt.refresh-token-expiration-millis}")
    private long refreshTokenExpirationMillis;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException{
        // 구글 로그인 성공 후 사용자 정보(principal) 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email"); // 구글이 넘겨준 이메일
        String name = oAuth2User.getAttribute("name");

        // 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // JWT 토큰 생성(JwtTokenProvider 사용)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(),user.getEmail(),user.getRoleKey());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        log.info("Google Login Success! JWT Token: {}", accessToken);

        // Refresh Token을 Redis에 저장
        refreshTokenRepository.save(new RefreshToken(String.valueOf(user.getId()), refreshToken));

        // Refresh Token을 HttpOnly Cookie에 저장 (보안 강화)
        // CookieUtil 사용 (시간은 초 단위 변환)
        int cookieMaxAge = (int) (refreshTokenExpirationMillis / 1000);
        cookieUtil.deleteCookie(response); // 기존 쿠키 삭제
        cookieUtil.addCookie(response, refreshToken, cookieMaxAge);

        // 리다이렉트 URI 설정 (로그인 성공 시 토큰을 쿼리 파라미터에 담아 전달)
        //"/login-success" 또는 메인 페이지로 설정해야함! 현재 test 시 화면 이동이 되지 않지만 DB에는 저장됨
        String targetUrl = UriComponentsBuilder.fromUriString("/login-success")
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        // 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
