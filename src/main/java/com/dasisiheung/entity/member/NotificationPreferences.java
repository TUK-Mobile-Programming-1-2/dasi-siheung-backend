package com.dasisiheung.entity.member;

import java.time.LocalTime;

/**
 * 알림 설정 값 묶음. 저장한 적이 없는 회원은 {@link #DEFAULT} 를 쓴다. (Android NotificationSettings 기본값과 같다)
 *
 * @param push 푸시 전체 스위치. 꺼져 있으면 분류별 설정과 관계없이 보내지 않는다
 */
public record NotificationPreferences(
        boolean push,
        boolean chat,
        boolean activity,
        boolean favoriteSpace,
        boolean marketing,
        DoNotDisturb doNotDisturb
) {

    public static final NotificationPreferences DEFAULT = new NotificationPreferences(
            true, true, true, false, false, DoNotDisturb.of(true, 22, 8));

    public NotificationPreferences {
        if (doNotDisturb == null) {
            throw new IllegalArgumentException("방해금지 설정이 없습니다.");
        }
    }

    /**
     * 서버가 푸시를 보내기 전에 확인한다. 방해금지 시간에는 분류와 관계없이 보내지 않는다.
     *
     * @param now 서비스 시간대(KST) 기준 현재 시각 — {@code LocalTime.now(clock)}
     */
    public boolean allowsPush(NotificationCategory category, LocalTime now) {
        return push && isEnabled(category) && !doNotDisturb.covers(now);
    }

    public boolean isEnabled(NotificationCategory category) {
        return switch (category) {
            case CHAT -> chat;
            case ACTIVITY -> activity;
            case FAVORITE_SPACE -> favoriteSpace;
            case MARKETING -> marketing;
        };
    }
}
