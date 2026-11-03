package com.dasisiheung.scheduler;

import com.dasisiheung.dto.privacy.PurgeResultResponse;
import com.dasisiheung.service.PrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 새벽 통계를 쌓고 보관 기간이 지난 개인정보를 파기한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivacyPurgeScheduler {

    private final PrivacyService privacyService;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        PurgeResultResponse result = privacyService.purge(null);
        log.info("개인정보 파기: 통계 {}건, 비식별화 {}건, 대화 삭제 {}건, 활동 사진 삭제 {}건",
                result.countedBookings(), result.anonymizedApplications(), result.deletedConversations(),
                result.deletedActivityPhotos());
    }
}
