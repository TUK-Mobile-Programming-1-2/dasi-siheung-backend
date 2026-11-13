package com.dasisiheung.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

class FcmPushSenderTest {

    private static final KeyPair KEY_PAIR = TestServiceAccounts.keyPair();
    private static final String SEND_URL =
            "https://fcm.googleapis.com/v1/projects/" + TestServiceAccounts.PROJECT_ID + "/messages:send";
    private static final PushMessage MESSAGE = new PushMessage("김시흥", "안녕하세요",
            Map.of("type", "CHAT_MESSAGE", "conversationId", "5"), "chat", "conversation-5");
    private static final String UNREGISTERED = """
            {"error": {"code": 404, "status": "NOT_FOUND", "message": "Requested entity was not found.",
              "details": [{"@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", "errorCode": "UNREGISTERED"}]}}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T11:00:00Z"));
    private MockRestServiceServer server;
    private FcmPushSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        FirebaseServiceAccount account = FirebaseServiceAccount.parse(TestServiceAccounts.json(KEY_PAIR), objectMapper);
        sender = new FcmPushSender(account.projectId(), new GoogleAccessTokenProvider(account, restClient, clock),
                restClient, objectMapper);
    }

    @Test
    @DisplayName("서명한 JWT 로 액세스 토큰을 받고, 기기마다 FCM v1 형식으로 보낸다")
    void send() {
        expectAccessToken("ya29.first");
        expectSend("phone", "ya29.first").andRespond(withSuccess("{\"name\": \"projects/p/messages/1\"}",
                MediaType.APPLICATION_JSON));
        expectSend("tablet", "ya29.first").andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        PushResult result = sender.send(List.of("phone", "tablet"), MESSAGE);

        assertThat(result.sentCount()).isEqualTo(2);
        assertThat(result.invalidTokens()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("액세스 토큰은 만료 1분 전까지 다시 쓰고, 그 뒤에는 새로 받는다")
    void reuseAccessToken() {
        expectAccessToken("ya29.first");
        expectSend("phone", "ya29.first").andRespond(withSuccess());
        expectSend("phone", "ya29.first").andRespond(withSuccess());
        expectAccessToken("ya29.second");
        expectSend("phone", "ya29.second").andRespond(withSuccess());

        sender.send(List.of("phone"), MESSAGE);
        clock.advance(Duration.ofMinutes(57));
        sender.send(List.of("phone"), MESSAGE);
        clock.advance(Duration.ofMinutes(2));
        sender.send(List.of("phone"), MESSAGE);

        server.verify();
    }

    @Test
    @DisplayName("UNREGISTERED 토큰은 지울 대상으로 돌려주고, 일시 장애는 로그만 남긴다")
    void classifyErrors() {
        expectAccessToken("ya29.first");
        expectSend("deleted-app", "ya29.first").andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON).body(UNREGISTERED));
        expectSend("phone", "ya29.first").andRespond(withServerError()
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\": {\"status\": \"UNAVAILABLE\"}}"));
        expectSend("tablet", "ya29.first").andRespond(withSuccess());

        PushResult result = sender.send(List.of("deleted-app", "phone", "tablet"), MESSAGE);

        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly("deleted-app");
        server.verify();
    }

    @Test
    @DisplayName("FCM 이 401 이면 다음 발송 때 액세스 토큰을 새로 받는다")
    void refreshAfterUnauthorized() {
        expectAccessToken("ya29.revoked");
        expectSend("phone", "ya29.revoked").andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\": {\"status\": \"UNAUTHENTICATED\"}}"));
        expectAccessToken("ya29.fresh");
        expectSend("phone", "ya29.fresh").andRespond(withSuccess());

        assertThat(sender.send(List.of("phone"), MESSAGE).sentCount()).isZero();
        assertThat(sender.send(List.of("phone"), MESSAGE).sentCount()).isEqualTo(1);
        server.verify();
    }

    @Test
    @DisplayName("액세스 토큰을 받지 못하면 보내지 않고 예외도 던지지 않는다")
    void tokenFailure() {
        server.expect(once(), requestTo(TestServiceAccounts.TOKEN_URI)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        server.expect(never(), requestTo(SEND_URL));

        PushResult result = sender.send(List.of("phone"), MESSAGE);

        assertThat(result.sentCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("기기가 없으면 아무것도 호출하지 않는다")
    void noTokens() {
        assertThat(sender.send(List.of(), MESSAGE)).isEqualTo(PushResult.none());
        server.verify();
    }

    private void expectAccessToken(String accessToken) {
        server.expect(once(), requestTo(TestServiceAccounts.TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(signedAssertion())
                .andRespond(withSuccess("{\"access_token\": \"%s\", \"expires_in\": 3600, \"token_type\": \"Bearer\"}"
                        .formatted(accessToken), MediaType.APPLICATION_JSON));
    }

    private org.springframework.test.web.client.ResponseActions expectSend(String token, String accessToken) {
        return server.expect(once(), requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.message.token").value(token))
                .andExpect(jsonPath("$.message.notification.title").value("김시흥"))
                .andExpect(jsonPath("$.message.notification.body").value("안녕하세요"))
                .andExpect(jsonPath("$.message.data.type").value("CHAT_MESSAGE"))
                .andExpect(jsonPath("$.message.data.conversationId").value("5"))
                .andExpect(jsonPath("$.message.android.priority").value("HIGH"))
                .andExpect(jsonPath("$.message.android.notification.channel_id").value("chat"))
                .andExpect(jsonPath("$.message.android.notification.tag").value("conversation-5"));
    }

    /** jwt-bearer 요청이고, assertion 이 서비스 계정 키로 서명돼 필요한 값을 담았는지 공개 키로 검증한다. */
    private RequestMatcher signedAssertion() {
        return request -> {
            String form = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
            Map<String, String> fields = new java.util.HashMap<>();
            for (String pair : form.split("&")) {
                String[] keyValue = pair.split("=", 2);
                fields.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
            }
            assertThat(fields.get("grant_type")).isEqualTo("urn:ietf:params:oauth:grant-type:jwt-bearer");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(fields.get("assertion").split("\\.")[1]),
                    StandardCharsets.UTF_8);
            assertThat(objectMapper.readTree(payload).get("aud").isTextual())
                    .as("Google 은 aud 를 문자열 하나로만 받는다: %s", payload).isTrue();
            Claims claims = Jwts.parser().verifyWith(KEY_PAIR.getPublic())
                    .clock(() -> java.util.Date.from(clock.instant()))
                    .build().parseSignedClaims(fields.get("assertion")).getPayload();
            assertThat(claims.getIssuer()).isEqualTo(TestServiceAccounts.CLIENT_EMAIL);
            assertThat(claims.getAudience()).containsExactly(TestServiceAccounts.TOKEN_URI);
            assertThat(claims.get("scope", String.class)).isEqualTo(GoogleAccessTokenProvider.SCOPE);
        };
    }

    /** 액세스 토큰 만료를 시험하려고 시각을 앞으로 돌릴 수 있는 Clock. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Seoul");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
