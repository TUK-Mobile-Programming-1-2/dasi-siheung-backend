package com.dasisiheung.push;

import java.util.Map;

/**
 * 기기에 보낼 알림 한 건.
 *
 * @param title     알림 제목
 * @param body      알림 본문
 * @param data      앱이 받아서 화면 이동 등에 쓰는 값 (모두 문자열)
 * @param channelId Android 알림 채널. 앱에 없는 채널이면 FCM 기본 채널로 표시된다
 * @param tag       같은 tag 의 알림은 새 알림으로 바뀐다 (예: 대화방별로 하나만 보이게). 없으면 null
 */
public record PushMessage(String title, String body, Map<String, String> data, String channelId, String tag) {

    public PushMessage {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
