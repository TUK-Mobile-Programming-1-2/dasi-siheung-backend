package com.dasisiheung.push;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 서비스 계정으로 Google OAuth2 액세스 토큰을 받는다. (FCM HTTP v1 호출용)
 * <p>
 * 서명한 JWT 를 token_uri 에 보내 1시간짜리 토큰을 받고, 만료 1분 전까지 재사용한다.
 * firebase-admin SDK 대신 직접 구현해 Firestore · Cloud Storage 같은 무거운 의존성을 들이지 않는다.
 */
class GoogleAccessTokenProvider {

    static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final Duration ASSERTION_LIFETIME = Duration.ofHours(1);
    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(1);

    private final FirebaseServiceAccount account;
    private final PrivateKey privateKey;
    private final RestClient restClient;
    private final Clock clock;

    private String cachedToken;
    private Instant cachedUntil = Instant.MIN;

    GoogleAccessTokenProvider(FirebaseServiceAccount account, RestClient restClient, Clock clock) {
        this.account = account;
        this.privateKey = account.privateKey();
        this.restClient = restClient;
        this.clock = clock;
    }

    /** @throws org.springframework.web.client.RestClientException 토큰을 받지 못했을 때 */
    synchronized String accessToken() {
        Instant now = clock.instant();
        if (cachedToken == null || !now.isBefore(cachedUntil)) {
            TokenResponse response = requestToken(now);
            cachedToken = response.accessToken();
            cachedUntil = now.plusSeconds(response.expiresIn()).minus(REFRESH_MARGIN);
        }
        return cachedToken;
    }

    /** FCM 이 401 로 답하면 다음 발송 때 새로 받는다. */
    synchronized void invalidate() {
        cachedToken = null;
    }

    private TokenResponse requestToken(Instant now) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("assertion", assertion(now));
        TokenResponse response = restClient.post()
                .uri(account.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Google 토큰 응답에 access_token 이 없습니다.");
        }
        return response;
    }

    private String assertion(Instant now) {
        return Jwts.builder()
                .header().keyId(account.privateKeyId()).and()
                .issuer(account.clientEmail())
                .subject(account.clientEmail())
                .audience().add(account.tokenUri()).and()
                .claim("scope", SCOPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ASSERTION_LIFETIME)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }
}
