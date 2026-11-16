package com.dasisiheung.push;

import java.util.Map;

/**
 * 기기에 보낼 알림 한 건. 두 방식으로 보낸다.
 * <ul>
 *   <li>{@link #notification}: 앱이 백그라운드면 시스템이 바로 알림을 그린다. data 는 알림을 눌렀을 때 화면 이동에 쓴다.</li>
 *   <li>{@link #dataOnly}: 앱이 onMessageReceived 로 받아 직접 알림을 그린다. 메시지 쌓기 · 알림 버튼처럼
 *       앱이 알림 모양을 정해야 할 때 쓴다. 채널 · tag 도 앱이 정한다.</li>
 * </ul>
 *
 * @param display 시스템이 그릴 알림. data-only 면 null
 * @param data    앱이 받는 값 (모두 문자열)
 */
public record PushMessage(Display display, Map<String, String> data) {

    public PushMessage {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    /**
     * @param channelId Android 알림 채널. 앱에 없는 채널이면 FCM 기본 채널로 표시된다
     * @param tag       같은 tag 의 알림은 새 알림으로 바뀐다 (예: 신청마다 하나만 보이게). 없으면 null
     */
    public static PushMessage notification(String title, String body, Map<String, String> data, String channelId,
                                           String tag) {
        return new PushMessage(new Display(title, body, channelId, tag), data);
    }

    public static PushMessage dataOnly(Map<String, String> data) {
        return new PushMessage(null, data);
    }

    public boolean isDataOnly() {
        return display == null;
    }

    /** 시스템이 그리는 알림의 제목 · 본문 · 채널 · tag. */
    public record Display(String title, String body, String channelId, String tag) {
    }
}
