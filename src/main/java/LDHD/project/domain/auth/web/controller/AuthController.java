package LDHD.project.domain.auth.web.controller;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import LDHD.project.common.response.GlobalResponse;
import LDHD.project.common.response.SuccessCode;
import LDHD.project.common.utils.CookieUtil;
import LDHD.project.domain.auth.service.AuthService;
import LDHD.project.domain.auth.web.controller.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name="Auth API", description = "인증/인가 관련 API(토큰 재발급, 로그아웃)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;



    @Operation(summary = "토큰 재발급", description= "쿠키의 Refresh Token을 이용해 Access Token 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<GlobalResponse> reissueToken(HttpServletRequest request, HttpServletResponse  response) {

        /*// 이 로그가 콘솔에 찍히는지(디버깅)
        System.out.println(">>> 1. Reissue 컨트롤러 진입 성공!");

        // 쿠키가 제대로 읽히는지
        String cookie = cookieUtil.getRefreshToken(request).orElse("쿠키 없음");
        System.out.println(">>> 2. 읽어온 쿠키: " + cookie);
*/
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = cookieUtil.getRefreshToken(request)
                .orElseThrow(()-> new GeneralException(ErrorCode.INVALID_TOKEN));

        // 2. 서비스 로직
        TokenResponse tokenDto = authService.reissueAccessToken(refreshToken);
        // 3. 새 Refresh 토큰을 쿠키에 갱신
        cookieUtil.addCookie(response, tokenDto.getRefreshToken(), tokenDto.getRefreshTokenMaxAge());

        return GlobalResponse.onSuccess(SuccessCode.OK, tokenDto.getAccessToken());
    }

    @Operation(summary = "로그아웃", description= "쿠키의 Refresh Token & 서버의 토큰 데이터를 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1. 쿠키에서 Refresh Token 꺼내기
        String refreshToken = cookieUtil.getRefreshToken(request)
                .orElseThrow(()-> new IllegalArgumentException("이미 로그아웃 상태입니다."));

        // 2. DB에서 삭제
        authService.logout(refreshToken);

        //3. 쿠키 삭제
        cookieUtil.deleteCookie(response);

        return GlobalResponse.onSuccess(SuccessCode.DELETED,"로그아웃 성공!");
    }
}
