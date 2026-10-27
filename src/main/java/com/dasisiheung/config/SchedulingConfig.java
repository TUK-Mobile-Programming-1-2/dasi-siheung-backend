package com.dasisiheung.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 정기 작업(만료 토큰 정리 등)은 scheduler 패키지에 둔다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
