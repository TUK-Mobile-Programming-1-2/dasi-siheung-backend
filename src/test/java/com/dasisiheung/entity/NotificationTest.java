package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.notification.Notification;
import com.dasisiheung.entity.notification.NotificationType;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.FixedClockConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final LocalDateTime NOW = FixedClockConfig.FIXED_LOCAL_NOW;
    private static final long RECIPIENT_ID = 5L;

    @Test
    @DisplayName("안 읽은 상태로 만들어지고, 받은 회원이 읽으면 처음 읽은 시각을 남긴다")
    void markRead() {
        Notification notification = notification();
        assertThat(notification.isRead()).isFalse();

        notification.markRead(RECIPIENT_ID, NOW);
        notification.markRead(RECIPIENT_ID, NOW.plusHours(1));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("남의 알림은 있는지 드러내지 않도록 NOTIFICATION_NOT_FOUND")
    void othersNotification() {
        Notification notification = notification();

        assertThatThrownBy(() -> notification.markRead(99L, NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        assertThat(notification.isRead()).isFalse();
    }

    private static Notification notification() {
        return new Notification(RECIPIENT_ID, NotificationType.PRICE_OFFER_RECEIVED, 31L, "공간 제공자의 역제안",
                "제공자님이 95,000원을 제안했어요.", NOW);
    }
}
