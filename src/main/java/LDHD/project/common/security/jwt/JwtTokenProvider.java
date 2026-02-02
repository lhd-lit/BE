package LDHD.project.common.security.jwt;

import LDHD.project.common.exception.GeneralException;
import LDHD.project.common.response.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.util.StringUtils;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j // 로그 출력
@Component // 빈으로 등록
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "role";
    private static final String EMAIL_KEY = "email";
    private static final String BEARER_TYPE = "Bearer";
    private static final String TOKEN_TYPE_KEY = "tokenType";

    private Key key;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration-millis}")
    private long accessTokenExpirationMillis;

    @Value("${jwt.refresh-token-expiration-millis}")
    private long refreshTokenExpirationMillis;

    @PostConstruct
    protected void init() {
        // yml에 설정한 secretkey 를 디코딩 -> 바이트 배열로 만듦
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        // 바이트 배열 사용해 암호화 키 객체 생성
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성 : 로그인 성공 시 이메일과 권한 받아 JWT 만듦
    public String generateAccessToken(Long userId, String email, String role){

        // Claims: 토큰 내부 정보를 담을 객체 / 토큰 주체를 이메일로 설정 -> 나중에 이메일로 권한 확인 위함
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put(AUTHORITIES_KEY, role);
        claims.put(EMAIL_KEY, email);
        claims.put(TOKEN_TYPE_KEY, "ACCESS");

        return buildToken(claims, accessTokenExpirationMillis);
    }

    // refresh 토큰 생성
    public String generateRefreshToken(Long userId){
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put(TOKEN_TYPE_KEY,"REFRESH");

        return buildToken(claims, refreshTokenExpirationMillis);
    }

    private String buildToken(Claims claims, long expireTime){
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime()+ expireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 인증 정보 조회
    public Authentication getAuthentication(String token){
        Claims claims = parseClaims(token);

        if(claims.get(AUTHORITIES_KEY)==null){
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }
        // 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }
    // 토큰에서 userId 추출
    public long getUserId(String token){
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 토큰 유효성 + 만료일자 확인
    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(key) // 비밀키로 검증
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (SecurityException | MalformedJwtException e){
            log.warn("잘못된 JWT 서명입니다.");
        }catch (ExpiredJwtException e){
            log.warn("만료된 JWT 토큰입니다.");
        }catch (UnsupportedJwtException e){
            log.warn("지원되지 않는 JWT 토큰입니다.");
        }catch (IllegalArgumentException e){
            log.warn("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
    // Token 타입 검증(ACCESS인지 REFRESH인지) -> refresh로 api 접근 방지 위함 => Filter에서 거를 수 있도록
    public void validateTokenType(String token, String expectedType) {
        Claims claims = parseClaims(token);

        String tokenType = claims.get(TOKEN_TYPE_KEY, String.class);

        if (tokenType == null || !tokenType.equals(expectedType)) {
            throw new GeneralException(ErrorCode.INVALID_TOKEN);
        }
    }

    // Claims 파싱 (만료된 토큰이어도 Claims는 꺼내올 수 있도록 처리)
    public Claims parseClaims(String token){
        try{
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e){ // 만료된 경우에 Claim 반환
            return e.getClaims();
        }
    }

    // Request Header에서 토큰 추출 (필터에서 사용)
    public String resolveToken(HttpServletRequest request) {
        // "Authorization: Bearer abcd.efgh.ijkl" 형태에서 "Bearer(권한을 달라는 방식)"를 떼어내는 작업
        // => 불필요한 Bearer 제거, 순수 토큰인 abcd.efgh.ijkl 값만 추출하도록
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_TYPE + " ")) {

            return bearerToken.substring(7);
        }
        return null;
    }
}
