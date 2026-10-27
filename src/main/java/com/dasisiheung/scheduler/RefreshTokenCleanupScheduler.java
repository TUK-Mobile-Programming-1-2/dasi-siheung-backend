package com.dasisiheung.scheduler;

import com.dasisiheung.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 만료된 리프레시 토큰을 매일 새벽 정리한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final AuthService authService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredRefreshTokens() {
        int deleted = authService.deleteExpiredRefreshTokens();
        log.info("만료된 리프레시 토큰 {}건 정리", deleted);
    }
}
