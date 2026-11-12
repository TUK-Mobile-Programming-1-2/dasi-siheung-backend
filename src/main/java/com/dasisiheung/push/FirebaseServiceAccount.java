package com.dasisiheung.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 로 받은 JSON 중 필요한 값.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FirebaseServiceAccount(
        @JsonProperty("project_id") String projectId,
        @JsonProperty("client_email") String clientEmail,
        @JsonProperty("private_key_id") String privateKeyId,
        @JsonProperty("private_key") String privateKeyPem,
        @JsonProperty("token_uri") String tokenUri
) {

    private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

    public FirebaseServiceAccount {
        tokenUri = tokenUri == null || tokenUri.isBlank() ? DEFAULT_TOKEN_URI : tokenUri;
    }

    /**
     * @param value base64 로 감싼 JSON 또는 JSON 그대로
     * @throws IllegalArgumentException 읽을 수 없거나 필수 값(project_id · client_email · private_key)이 없을 때
     */
    public static FirebaseServiceAccount parse(String value, ObjectMapper objectMapper) {
        String trimmed = value.strip();
        String json = trimmed.startsWith("{")
                ? trimmed
                : new String(Base64.getMimeDecoder().decode(trimmed), StandardCharsets.UTF_8);
        FirebaseServiceAccount account;
        try {
            account = objectMapper.readValue(json, FirebaseServiceAccount.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Firebase 서비스 계정 키(JSON)를 읽을 수 없습니다.", e);
        }
        if (isBlank(account.projectId()) || isBlank(account.clientEmail()) || isBlank(account.privateKeyPem())) {
            throw new IllegalArgumentException("Firebase 서비스 계정 키에 project_id · client_email · private_key 가 없습니다.");
        }
        account.privateKey();
        return account;
    }

    /** @throws IllegalArgumentException PKCS#8 RSA 키가 아닐 때 */
    public PrivateKey privateKey() {
        String base64 = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Firebase 서비스 계정의 private_key 를 읽을 수 없습니다.", e);
        }
    }

    /** 비공개 키가 로그에 남지 않도록 프로젝트 · 계정만 보여준다. */
    @Override
    public String toString() {
        return "FirebaseServiceAccount[projectId=%s, clientEmail=%s]".formatted(projectId, clientEmail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
