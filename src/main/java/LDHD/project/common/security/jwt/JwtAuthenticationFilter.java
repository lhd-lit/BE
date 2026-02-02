package LDHD.project.common.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {

        // 필터 들어가기 전 이전 인증 정보 초기화
        SecurityContextHolder.clearContext();

        try {
            // Request 헤더 부에서 JWT token 추출
            String token = jwtTokenProvider.resolveToken(request);
            // validateToken으로 토큰 유효성 검사
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                // ACCESS 토큰인지 검증(Provider 클래스의 validateTokenType 사용) =>예외 발생시 catch 블럭으로 이동
                jwtTokenProvider.validateTokenType(token, "ACCESS");

                // 토큰이 유효하다면 인증 객체(Authentication)를 받아옴
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}",
                        authentication.getName(), request.getRequestURI());
            }

        } catch (Exception e){
            SecurityContextHolder.clearContext();
            log.warn("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
