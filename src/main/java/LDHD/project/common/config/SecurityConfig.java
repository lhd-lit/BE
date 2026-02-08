package LDHD.project.common.config;

import LDHD.project.common.security.jwt.JwtAuthenticationFilter;
import LDHD.project.common.security.jwt.JwtTokenProvider;
import LDHD.project.common.security.oauth.OAuth2SuccessHandler;
import LDHD.project.domain.user.Role;
import LDHD.project.domain.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@EnableWebSecurity// spring security 기능 활성화
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. csrf 보안 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 2. CORS 설정 (프론트엔드 통신 허용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 3. 기존 로그인 방식 비활성화(Http Basic, Form)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // 4. H2 Console 설정
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // 5. 세션 관리 정책 설정 -> STATLESS: 우리 프로젝트에선 세션 방식 X
                .sessionManagement(management
                        -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 6. URL 별 권한 관리
                .authorizeHttpRequests(auth -> auth
                        // 정적 자원(css, js, image), 메인 페이지, 로그인 관련 URL 모두 허용
                        .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/h2-console/**"). permitAll()
                        // feat/create/login 작성 후 swagger 연동 하려고 보니 security 상에서 로그인 화면을 계속 띄워 접근 제한
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**","/swagger-ui.html", "/api/selfStudy/health").permitAll()
                        // 로그인, 토큰 재발급(인증 없이 접근 허용)
                        .requestMatchers("/login/**","/api/auth/**", "/api/auth/reissue").permitAll()
                        // "/api/**"로 시작하는 요청은 인증된 유저만 접근 가능(USER 권한 필요)
                        .requestMatchers("/api/**").hasRole(Role.USER.name())
                        // 그 외 나머지 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 7. OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        // 사용자 정보 가져오기(DB 저장)
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 로그인 성공 시 -> JWT 발급
                        .successHandler(oAuth2SuccessHandler)
                )
                // JWT 인증 필터 적용(기본 로그인 필터 앞에서 먼저 JWT 토큰 인증 할 수 있도록)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 허용 설정(프론트와 연동)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 주소 허용
        configuration.setAllowedOriginPatterns(List.of("http://localhost:5173")); // 실제 프론트엔드 도메인 주소 넣기
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true); // 쿠키/인증정보 포함 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
