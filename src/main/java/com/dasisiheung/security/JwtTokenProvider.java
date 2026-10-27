package com.dasisiheung.security;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰(JWT, HS256)을 발급·검증한다.
 * <pre>
 * sub  = 회원 ID
 * role = 역할 (USER / PROVIDER / ADMIN)
 * </pre>
 */
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenValidity;
    private final Clock clock;
    private final JwtParser parser;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = properties.accessTokenValidity();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    public String createAccessToken(Long memberId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * @throws InvalidJwtException 만료({@link ErrorCode#EXPIRED_TOKEN}) 또는 위조·손상({@link ErrorCode#INVALID_TOKEN})
     */
    public AuthMember parseAccessToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return new AuthMember(parseMemberId(claims), parseRole(claims));
        } catch (ExpiredJwtException e) {
            throw new InvalidJwtException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Duration accessTokenValidity() {
        return accessTokenValidity;
    }

    private static Long parseMemberId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null) {
            throw new IllegalArgumentException("sub 클레임이 없습니다.");
        }
        return Long.valueOf(subject);
    }

    private static Role parseRole(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);
        if (role == null) {
            throw new IllegalArgumentException("role 클레임이 없습니다.");
        }
        return Role.valueOf(role);
    }
}
