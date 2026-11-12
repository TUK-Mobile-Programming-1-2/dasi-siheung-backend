package com.dasisiheung.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 응답을 늦추지 않아야 하는 부가 작업(푸시 발송)을 @Async 로 따로 실행한다.
 * 실행기는 Spring Boot 가 만드는 applicationTaskExecutor (spring.task.execution.*) 를 쓴다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
