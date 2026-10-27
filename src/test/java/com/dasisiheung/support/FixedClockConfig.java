package com.dasisiheung.support;

import com.dasisiheung.config.ClockConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * REST Docs 테스트의 현재 시각을 고정한다. (2026-10-20 21:00 KST)
 * 문서 예시 값과 JWT 발급·검증 시각이 매번 같아진다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfig {

    public static final Instant FIXED_NOW = Instant.parse("2026-10-20T12:00:00Z");
    public static final LocalDateTime FIXED_LOCAL_NOW = LocalDateTime.ofInstant(FIXED_NOW, ClockConfig.SERVICE_ZONE);
    public static final LocalDate FIXED_TODAY = FIXED_LOCAL_NOW.toLocalDate();

    @Bean
    public Clock clock() {
        return fixedClock();
    }

    public static Clock fixedClock() {
        return Clock.fixed(FIXED_NOW, ClockConfig.SERVICE_ZONE);
    }
}
