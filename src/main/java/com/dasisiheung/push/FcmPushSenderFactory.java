package com.dasisiheung.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

/** 서비스 계정 키로 FCM 발송기를 만든다. (토큰 발급기 · 발송기는 패키지 안에서만 쓰므로 여기서 조립한다) */
@Slf4j
public final class FcmPushSenderFactory {

    private FcmPushSenderFactory() {
    }

    /** @throws IllegalArgumentException 서비스 계정 키를 읽을 수 없을 때 */
    public static FcmPushSender create(String credentials, RestClient restClient, ObjectMapper objectMapper,
                                       Clock clock) {
        FirebaseServiceAccount account = FirebaseServiceAccount.parse(credentials, objectMapper);
        log.info("FCM 푸시 사용: 프로젝트 {}", account.projectId());
        GoogleAccessTokenProvider tokenProvider = new GoogleAccessTokenProvider(account, restClient, clock);
        return new FcmPushSender(account.projectId(), tokenProvider, restClient, objectMapper);
    }
}
