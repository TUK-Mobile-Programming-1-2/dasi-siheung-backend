package com.dasisiheung.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정. 비밀 키는 환경 변수 JWT_SECRET 으로 주입하며, 짧으면 서버가 기동되지 않는다.
 *
 * @param secret               HS256 서명 키 (32바이트 이상)
 * @param accessTokenValidity  액세스 토큰 유효 시간 (기본 30분)
 * @param refreshTokenValidity 리프레시 토큰 유효 시간 (기본 14일)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity, Duration refreshTokenValidity) {

    private static final int MIN_SECRET_BYTES = 32;
    private static final Duration DEFAULT_ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(30);
    private static final Duration DEFAULT_REFRESH_TOKEN_VALIDITY = Duration.ofDays(14);

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret(환경 변수 JWT_SECRET)은 32바이트 이상이어야 합니다.");
        }
        accessTokenValidity = accessTokenValidity == null ? DEFAULT_ACCESS_TOKEN_VALIDITY : accessTokenValidity;
        refreshTokenValidity = refreshTokenValidity == null ? DEFAULT_REFRESH_TOKEN_VALIDITY : refreshTokenValidity;
    }
}
