package com.dasisiheung.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-jwt-secret-key-for-dasi-siheung-0123456789";
    private static final Instant ISSUED_AT = Instant.parse("2026-10-20T12:00:00Z");

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14));

    @Test
    @DisplayName("발급한 토큰에서 회원 ID 와 역할을 그대로 꺼낸다")
    void createAndParse() {
        JwtTokenProvider provider = providerAt(ISSUED_AT);

        AuthMember authMember = provider.parseAccessToken(provider.createAccessToken(42L, Role.PROVIDER));

        assertThat(authMember).isEqualTo(new AuthMember(42L, Role.PROVIDER));
        assertThat(authMember.authorities()).extracting("authority").containsExactly("ROLE_PROVIDER");
    }

    @Test
    @DisplayName("유효 시간이 지난 토큰은 EXPIRED_TOKEN")
    void expiredToken() {
        String token = providerAt(ISSUED_AT).createAccessToken(1L, Role.USER);
        JwtTokenProvider later = providerAt(ISSUED_AT.plus(Duration.ofMinutes(31)));

        assertThatThrownBy(() -> later.parseAccessToken(token))
                .isInstanceOf(InvalidJwtException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("다른 키로 서명했거나 형식이 깨진 토큰은 INVALID_TOKEN")
    void invalidToken() {
        JwtTokenProvider provider = providerAt(ISSUED_AT);
        JwtProperties otherKey = new JwtProperties("another-secret-key-another-secret-key-0000", null, null);
        String forged = new JwtTokenProvider(otherKey, fixed(ISSUED_AT)).createAccessToken(1L, Role.ADMIN);

        assertThatThrownBy(() -> provider.parseAccessToken(forged))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
        assertThatThrownBy(() -> provider.parseAccessToken("not.a.jwt"))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("32바이트보다 짧은 비밀 키는 기동 단계에서 거부한다")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short", null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("유효 시간을 지정하지 않으면 30분 / 14일")
    void defaultValidity() {
        JwtProperties defaults = new JwtProperties(SECRET, null, null);

        assertThat(defaults.accessTokenValidity()).isEqualTo(Duration.ofMinutes(30));
        assertThat(defaults.refreshTokenValidity()).isEqualTo(Duration.ofDays(14));
    }

    private JwtTokenProvider providerAt(Instant now) {
        return new JwtTokenProvider(properties, fixed(now));
    }

    private static Clock fixed(Instant now) {
        return Clock.fixed(now, ClockConfig.SERVICE_ZONE);
    }
}
