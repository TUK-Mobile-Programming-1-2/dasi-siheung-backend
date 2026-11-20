package com.dasisiheung.dto.notification;

import com.dasisiheung.entity.notification.Notification;
import com.dasisiheung.entity.notification.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 알림함 항목. 누르면 applicationId 의 신청 상세를 연다. */
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "12") Long id,
        @Schema(description = "알림 종류 — 푸시 data 의 type 과 같다", example = "PRICE_OFFER_RECEIVED") NotificationType type,
        @Schema(description = "제목", example = "공간 제공자의 역제안") String title,
        @Schema(description = "본문", example = "김제공님이 '정왕 시장 골목 1층' 11/21~11/22 이용 금액으로 95,000원을 제안했어요.") String body,
        @Schema(description = "연결된 신청 ID (신청 상세로 이동)", example = "31") Long applicationId,
        @Schema(description = "읽었는지", example = "false") boolean read,
        @Schema(description = "처음 읽은 시각 (안 읽었으면 null)") LocalDateTime readAt,
        @Schema(description = "알림 시각") LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getApplicationId(), notification.isRead(),
                notification.getReadAt(), notification.getCreatedAt());
    }
}
