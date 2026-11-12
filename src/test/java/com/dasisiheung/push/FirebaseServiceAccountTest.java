package com.dasisiheung.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FirebaseServiceAccountTest {

    private static final KeyPair KEY_PAIR = TestServiceAccounts.keyPair();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("base64 로 감싼 값과 JSON 그대로 모두 읽는다")
    void parse() {
        FirebaseServiceAccount fromBase64 = FirebaseServiceAccount.parse(TestServiceAccounts.base64(KEY_PAIR), objectMapper);
        FirebaseServiceAccount fromJson = FirebaseServiceAccount.parse(TestServiceAccounts.json(KEY_PAIR), objectMapper);

        assertThat(fromBase64).isEqualTo(fromJson);
        assertThat(fromBase64.projectId()).isEqualTo(TestServiceAccounts.PROJECT_ID);
        assertThat(fromBase64.clientEmail()).isEqualTo(TestServiceAccounts.CLIENT_EMAIL);
        assertThat(fromBase64.privateKey().getEncoded()).isEqualTo(KEY_PAIR.getPrivate().getEncoded());
    }

    @Test
    @DisplayName("token_uri 가 없으면 Google 기본 주소를 쓴다")
    void defaultTokenUri() {
        String json = TestServiceAccounts.json(KEY_PAIR).replace("\"token_uri\": \"" + TestServiceAccounts.TOKEN_URI + "\"",
                "\"token_uri\": null");

        assertThat(FirebaseServiceAccount.parse(json, objectMapper).tokenUri()).isEqualTo("https://oauth2.googleapis.com/token");
    }

    @Test
    @DisplayName("JSON 이 아니거나 필수 값이 없거나 키가 깨졌으면 IllegalArgumentException")
    void invalid() {
        assertThatThrownBy(() -> FirebaseServiceAccount.parse("not-a-json", objectMapper))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FirebaseServiceAccount.parse("{\"project_id\": \"p\"}", objectMapper))
                .isInstanceOf(IllegalArgumentException.class);
        String brokenKey = TestServiceAccounts.json(KEY_PAIR).replaceAll("BEGIN PRIVATE KEY-----\\\\n[^\\\\]+",
                "BEGIN PRIVATE KEY-----\\\\nAAAA");
        assertThatThrownBy(() -> FirebaseServiceAccount.parse(brokenKey, objectMapper))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString 에 비공개 키가 나오지 않는다 (로그 유출 방지)")
    void toStringHidesKey() {
        FirebaseServiceAccount account = FirebaseServiceAccount.parse(TestServiceAccounts.json(KEY_PAIR), objectMapper);

        assertThat(account.toString())
                .contains(TestServiceAccounts.PROJECT_ID)
                .doesNotContain("PRIVATE KEY")
                .doesNotContain(account.privateKeyPem());
    }
}
