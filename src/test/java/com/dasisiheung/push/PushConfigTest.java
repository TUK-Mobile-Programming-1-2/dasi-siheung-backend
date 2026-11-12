package com.dasisiheung.push;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.config.PushConfig;
import com.dasisiheung.support.FixedClockConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PushConfigTest {

    private final PushConfig config = new PushConfig();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("키가 있으면 FCM 으로 보낸다")
    void fcm() {
        assertThat(senderWith(TestServiceAccounts.base64(TestServiceAccounts.keyPair())))
                .isInstanceOf(FcmPushSender.class);
    }

    @Test
    @DisplayName("키가 없으면 로그만 남긴다 (로컬 · 테스트 · CI)")
    void noCredentials() {
        assertThat(senderWith(null)).isInstanceOf(LoggingPushSender.class);
        assertThat(senderWith(" ")).isInstanceOf(LoggingPushSender.class);
    }

    @Test
    @DisplayName("키가 잘못돼도 서버는 뜨고 푸시만 꺼진다")
    void invalidCredentials() {
        assertThat(senderWith("not-a-key")).isInstanceOf(LoggingPushSender.class);
    }

    private PushSender senderWith(String credentials) {
        return config.pushSender(new PushProperties(credentials), RestClient.builder(), objectMapper,
                FixedClockConfig.fixedClock());
    }
}
