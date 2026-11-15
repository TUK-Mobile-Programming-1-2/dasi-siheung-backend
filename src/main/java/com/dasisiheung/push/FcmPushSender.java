package com.dasisiheung.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * FCM HTTP v1 API 로 기기마다 한 건씩 보낸다.
 * <p>
 * 앱 삭제 · 토큰 만료(UNREGISTERED), 다른 Firebase 프로젝트의 토큰(SENDER_ID_MISMATCH)은 결과의 invalidTokens 로 돌려줘
 * 호출한 쪽이 지우게 한다. 그 밖의 오류(일시 장애 · 할당량)는 로그만 남긴다.
 */
@Slf4j
public class FcmPushSender implements PushSender {

    static final String SEND_URL = "https://fcm.googleapis.com/v1/projects/{projectId}/messages:send";
    private static final Set<String> INVALID_TOKEN_ERRORS = Set.of("UNREGISTERED", "SENDER_ID_MISMATCH");

    private final String projectId;
    private final GoogleAccessTokenProvider accessTokenProvider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    FcmPushSender(String projectId, GoogleAccessTokenProvider accessTokenProvider, RestClient restClient,
                  ObjectMapper objectMapper) {
        this.projectId = projectId;
        this.accessTokenProvider = accessTokenProvider;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public PushResult send(List<String> tokens, PushMessage message) {
        if (tokens.isEmpty()) {
            return PushResult.none();
        }
        String accessToken;
        try {
            accessToken = accessTokenProvider.accessToken();
        } catch (RuntimeException e) {
            log.warn("FCM 액세스 토큰을 받지 못해 푸시를 보내지 않았습니다: {}", e.getMessage());
            return PushResult.none();
        }
        int sent = 0;
        List<String> invalidTokens = new ArrayList<>();
        for (String token : tokens) {
            switch (sendOne(accessToken, token, message)) {
                case SENT -> sent++;
                case INVALID_TOKEN -> invalidTokens.add(token);
                case FAILED -> { }
            }
        }
        return new PushResult(sent, invalidTokens);
    }

    private Outcome sendOne(String accessToken, String token, PushMessage message) {
        try {
            restClient.post()
                    .uri(SEND_URL, projectId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", payload(token, message)))
                    .retrieve()
                    .toBodilessEntity();
            return Outcome.SENT;
        } catch (RestClientResponseException e) {
            return handleError(e);
        } catch (RestClientException e) {
            log.warn("FCM 에 연결하지 못했습니다: {}", e.getMessage());
            return Outcome.FAILED;
        }
    }

    private Outcome handleError(RestClientResponseException e) {
        if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            accessTokenProvider.invalidate();
        }
        String errorCode = fcmErrorCode(e);
        if (INVALID_TOKEN_ERRORS.contains(errorCode)) {
            return Outcome.INVALID_TOKEN;
        }
        log.warn("FCM 발송 실패 ({} {}): {}", e.getStatusCode().value(), errorCode, e.getResponseBodyAsString());
        return Outcome.FAILED;
    }

    /** 응답 본문 error.details[].errorCode (예: UNREGISTERED). 없으면 error.status, 읽을 수 없으면 null. */
    private String fcmErrorCode(RestClientResponseException e) {
        JsonNode error;
        try {
            error = objectMapper.readTree(e.getResponseBodyAsString()).path("error");
        } catch (JsonProcessingException | IllegalArgumentException parseFailure) {
            return null;
        }
        for (JsonNode detail : error.path("details")) {
            if (detail.hasNonNull("errorCode")) {
                return detail.get("errorCode").asText();
            }
        }
        return error.path("status").asText(null);
    }

    /**
     * notification 이 있으면 앱이 백그라운드일 때 시스템이 바로 표시하고, data 는 눌렀을 때 화면 이동에 쓴다.
     * data-only 면 notification 블록 없이 보내 앱이 onMessageReceived 로 받아 직접 그린다.
     * 어느 쪽이든 잠든 기기도 바로 깨우도록 우선순위는 HIGH 로 보낸다.
     */
    static Map<String, Object> payload(String token, PushMessage message) {
        Map<String, Object> android = new LinkedHashMap<>();
        android.put("priority", "HIGH");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", token);
        if (!message.isDataOnly()) {
            PushMessage.Display display = message.display();
            payload.put("notification", Map.of("title", display.title(), "body", display.body()));
            android.put("notification", androidNotification(display));
        }
        payload.put("data", message.data());
        payload.put("android", android);
        return payload;
    }

    private static Map<String, Object> androidNotification(PushMessage.Display display) {
        Map<String, Object> notification = new LinkedHashMap<>();
        if (display.channelId() != null) {
            notification.put("channel_id", display.channelId());
        }
        if (display.tag() != null) {
            notification.put("tag", display.tag());
        }
        return notification;
    }

    private enum Outcome {
        SENT, INVALID_TOKEN, FAILED
    }
}
