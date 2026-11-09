package com.dasisiheung.dto.member;

import com.dasisiheung.entity.member.DoNotDisturb;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.dasisiheung.entity.member.NotificationSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NotificationSettingResponse(
        @Schema(description = "푸시 알림 전체", example = "true") boolean push,
        @Schema(description = "1:1 대화 알림", example = "true") boolean chat,
        @Schema(description = "활동 알림 (신청 · 가격 협의 · 승인 · 거절)", example = "true") boolean activity,
        @Schema(description = "찜한 공간 소식", example = "false") boolean favoriteSpace,
        @Schema(description = "광고성 정보 수신 동의", example = "false") boolean marketing,
        @Schema(description = "방해금지 사용", example = "true") boolean dndEnabled,
        @Schema(description = "방해금지 시작 시각 (0~23시, 포함)", example = "22") int dndStartHour,
        @Schema(description = "방해금지 끝 시각 (0~23시, 미포함)", example = "8") int dndEndHour,
        @Schema(description = "광고성 정보 수신 동의 시각 (KST). 동의하지 않았으면 null — 서버가 기록합니다")
        LocalDateTime marketingConsentedAt,
        @Schema(description = "마지막 저장 시각 (KST). 저장한 적이 없으면 null — 기본값입니다")
        LocalDateTime updatedAt
) {

    /** 저장한 적 없는 회원의 기본값. */
    public static NotificationSettingResponse defaults() {
        return of(NotificationPreferences.DEFAULT, null, null);
    }

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return of(setting.preferences(), setting.getMarketingConsentedAt(), setting.getUpdatedAt());
    }

    private static NotificationSettingResponse of(NotificationPreferences preferences, LocalDateTime marketingConsentedAt,
                                                  LocalDateTime updatedAt) {
        DoNotDisturb dnd = preferences.doNotDisturb();
        return new NotificationSettingResponse(preferences.push(), preferences.chat(), preferences.activity(),
                preferences.favoriteSpace(), preferences.marketing(), dnd.isEnabled(), dnd.getStartHour(),
                dnd.getEndHour(), marketingConsentedAt, updatedAt);
    }
}
