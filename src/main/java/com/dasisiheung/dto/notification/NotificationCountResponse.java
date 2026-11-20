package com.dasisiheung.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

/** 알림 수. 안 읽은 수(배지) · 모두 읽음으로 바꾼 수에 쓴다. */
public record NotificationCountResponse(
        @Schema(description = "알림 수", example = "2") long count
) {
}
