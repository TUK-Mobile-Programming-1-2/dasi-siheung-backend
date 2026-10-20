package com.dasisiheung.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각은 항상 주입받은 {@link Clock}으로 구한다. 테스트에서 고정 시각으로 바꿔 끼우기 위함이다.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }
}
