package com.dasisiheung.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 푸시 설정.
 *
 * @param firebaseCredentials Firebase 서비스 계정 키(JSON)를 base64 로 한 줄로 만든 값 (JSON 그대로도 된다).
 *                            비어 있으면 로그만 남기는 발송기를 쓴다.
 */
@ConfigurationProperties(prefix = "app.push")
public record PushProperties(String firebaseCredentials) {

    public boolean hasFirebaseCredentials() {
        return firebaseCredentials != null && !firebaseCredentials.isBlank();
    }
}
