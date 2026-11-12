package com.dasisiheung.push;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** 테스트마다 새로 만든 RSA 키로 가짜 Firebase 서비스 계정 JSON 을 만든다. (실제 키는 쓰지 않는다) */
final class TestServiceAccounts {

    static final String PROJECT_ID = "demo-project";
    static final String CLIENT_EMAIL = "firebase-adminsdk@demo-project.iam.gserviceaccount.com";
    static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    private TestServiceAccounts() {
    }

    static KeyPair keyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String json(KeyPair keyPair) {
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----\\n";
        return """
                {
                  "type": "service_account",
                  "project_id": "%s",
                  "private_key_id": "test-key-id",
                  "private_key": "%s",
                  "client_email": "%s",
                  "token_uri": "%s"
                }
                """.formatted(PROJECT_ID, pem, CLIENT_EMAIL, TOKEN_URI);
    }

    static String base64(KeyPair keyPair) {
        return Base64.getEncoder().encodeToString(json(keyPair).getBytes(StandardCharsets.UTF_8));
    }
}
