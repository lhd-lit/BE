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

    @Value("${spring.security.oauth2.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException{
        log.info("OAuth2SuccessHandler 호출됨 - Request URI: {}, Method: {}, Query String: {}", 
                request.getRequestURI(), request.getMethod(), request.getQueryString());
        log.info("OAuth2SuccessHandler 호출됨 - 이 메시지가 보이면 이미 인증된 상태입니다!");
        
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
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        log.info("리다이렉트 URL: {}", targetUrl);
        log.info("커스텀 프로토콜 사용 여부: {}", targetUrl.startsWith("lit://"));
        
        // 커스텀 프로토콜(lit://) 사용 시 302 리다이렉트로 직접 전달
        // 브라우저가 커스텀 프로토콜을 처리할 수 있도록 Location 헤더에 직접 설정
        if (targetUrl.startsWith("lit://")) {
            // 302 리다이렉트 (브라우저가 커스텀 프로토콜을 처리)
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", targetUrl);
            
            // 캐시 방지 (일부 환경에서 redirect 페이지 캐시로 꼬이는 경우 예방)
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            
            // 혹시 모를 CSP/프레임 문제 방지
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // 바디는 비워도 됨 (브라우저가 Location 따라감)
            response.getWriter().flush();
            
        } else {
            // 일반 HTTP URL인 경우 기존 방식 사용
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
