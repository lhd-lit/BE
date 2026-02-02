package LDHD.project.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_NAME = "refresh_token";
    private final boolean secure;

    public CookieUtil(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    // Refresh 토큰 쿠키 생성
    public void addCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_NAME, token)
                .path("/") // 쿠키 범위 최소화
                .httpOnly(true) // http 환경에서만 쿠키 전송
                .secure(false) // 배포 환경: true - None, 로컬: false - Lax
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // Refresh 토큰 쿠키 조회(null 안정성 위해 Optional<>)
    public Optional<String> getRefreshToken(HttpServletRequest request) {
        // 요청에 쿠키 없는 경우
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                // 쿠키 이름으로 필터링
                .filter(cookie -> REFRESH_TOKEN_NAME.equals(cookie.getName()))
                // 첫 번째 쿠키 반환
                .findFirst()
                // 토큰 값만 추출
                .map(jakarta.servlet.http.Cookie::getValue);
    }


    // Refresh 토큰 쿠키 삭제(로그아웃 시 사용)
    public void deleteCookie(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0) // 만료 시간 0으로 지정 -> 즉시 삭제
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());
    }
}