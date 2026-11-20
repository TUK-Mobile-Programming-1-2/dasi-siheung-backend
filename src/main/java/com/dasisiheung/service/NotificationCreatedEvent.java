package com.dasisiheung.service;

import com.dasisiheung.entity.notification.Notification;
import com.dasisiheung.entity.notification.NotificationType;
import java.time.LocalDateTime;

/**
 * 알림함에 알림이 남았다. 커밋된 뒤 받는 사람에게 같은 내용을 푸시로 보낸다. ({@link PushNotificationService})
 *
 * @param recipientId   받는 사람
 * @param applicationId 눌렀을 때 열 신청 (없으면 null)
 */
public record NotificationCreatedEvent(
        Long notificationId,
        Long recipientId,
        NotificationType type,
        Long applicationId,
        String title,
        String body,
        LocalDateTime createdAt
) {

    static NotificationCreatedEvent from(Notification notification) {
        return new NotificationCreatedEvent(notification.getId(), notification.getRecipientId(), notification.getType(),
                notification.getApplicationId(), notification.getTitle(), notification.getBody(),
                notification.getCreatedAt());
    }
}
