package com.dasisiheung.service;

import com.dasisiheung.entity.notification.NotificationType;

/**
 * 알림함에 남기고 푸시로 보낼 알림 한 건. ({@link NotificationService#send})
 *
 * @param recipientId   받는 회원
 * @param applicationId 눌렀을 때 열 신청
 */
public record NotificationDraft(
        Long recipientId,
        NotificationType type,
        Long applicationId,
        String title,
        String body
) {
}
