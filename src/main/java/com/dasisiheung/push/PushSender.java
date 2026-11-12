package com.dasisiheung.push;

import java.util.List;

/**
 * 기기 푸시 발송. 운영은 FCM, 키가 없는 로컬 · 테스트 · CI 는 로그만 남긴다. (app.push.firebase-credentials)
 * <p>
 * 푸시는 부가 기능이므로 통신 오류는 던지지 않고 로그로 남긴다. 알림을 못 받아도 앱은 폴링으로 새 메시지를 본다.
 */
public interface PushSender {

    PushResult send(List<String> tokens, PushMessage message);
}
