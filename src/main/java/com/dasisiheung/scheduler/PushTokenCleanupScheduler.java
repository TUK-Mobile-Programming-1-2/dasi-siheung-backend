package com.dasisiheung.scheduler;

import com.dasisiheung.service.PushTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 60일 넘게 다시 등록되지 않은 푸시 토큰(쓰지 않는 기기)을 매일 새벽 정리한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushTokenCleanupScheduler {

    private final PushTokenService pushTokenService;

    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    public void deleteStalePushTokens() {
        int deleted = pushTokenService.deleteStale();
        log.info("오래된 푸시 토큰 {}건 정리", deleted);
    }
}
