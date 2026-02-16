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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final CookieUtil cookieUtil;
    private final TemplateEngine templateEngine;

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
        
        // 커스텀 프로토콜(lit://) 사용 시 Thymeleaf 템플릿으로 HTML 페이지 반환
        // HTTP 리다이렉트는 커스텀 프로토콜을 직접 처리할 수 없으므로 HTML 페이지 필요
        if (targetUrl.startsWith("lit://")) {
            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            
            // Thymeleaf 컨텍스트 생성 및 변수 주입
            Context context = new Context();
            context.setVariable("accessToken", accessToken);
            
            // RequestContextHolder 설정 (Thymeleaf가 request, response 접근 가능하도록)
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
            
            // Thymeleaf 템플릿 렌더링
            String html = templateEngine.process("oauth-success", context);
            
            response.getWriter().write(html);
            response.getWriter().flush();
            
        } else {
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
}
