package com.dasisiheung.config;

import com.dasisiheung.push.FcmPushSenderFactory;
import com.dasisiheung.push.LoggingPushSender;
import com.dasisiheung.push.PushProperties;
import com.dasisiheung.push.PushSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * app.push.firebase-credentials(FIREBASE_CREDENTIALS) 가 있으면 FCM, 없으면 로그만 남기는 발송기를 쓴다.
 * 키가 잘못돼도 서버 기동은 막지 않는다. (ERROR 로그를 남기고 푸시만 끈다)
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PushProperties.class)
public class PushConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public PushSender pushSender(PushProperties properties, RestClient.Builder restClientBuilder,
                                 ObjectMapper objectMapper, Clock clock) {
        if (!properties.hasFirebaseCredentials()) {
            log.info("FIREBASE_CREDENTIALS 가 없어 푸시는 로그로만 남깁니다.");
            return new LoggingPushSender();
        }
        try {
            RestClient restClient = restClientBuilder.requestFactory(requestFactory()).build();
            return FcmPushSenderFactory.create(properties.firebaseCredentials(), restClient, objectMapper, clock);
        } catch (IllegalArgumentException e) {
            log.error("FIREBASE_CREDENTIALS 를 읽지 못해 푸시를 끕니다: {}", e.getMessage());
            return new LoggingPushSender();
        }
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
