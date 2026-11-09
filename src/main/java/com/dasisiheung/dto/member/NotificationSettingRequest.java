package com.dasisiheung.dto.member;

import com.dasisiheung.entity.member.DoNotDisturb;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 전체 교체(PUT). 모든 항목을 보내야 한다. */
public record NotificationSettingRequest(
        @Schema(description = "푸시 알림 전체. false 면 아래 분류와 관계없이 보내지 않습니다", example = "true")
        @NotNull Boolean push,

        @Schema(description = "1:1 대화 알림", example = "true")
        @NotNull Boolean chat,

        @Schema(description = "활동 알림 (신청 · 가격 협의 · 승인 · 거절)", example = "true")
        @NotNull Boolean activity,

        @Schema(description = "찜한 공간 소식", example = "false")
        @NotNull Boolean favoriteSpace,

        @Schema(description = "광고성 정보 수신 동의 (이벤트 · 혜택)", example = "false")
        @NotNull Boolean marketing,

        @Schema(description = "방해금지 사용", example = "true")
        @NotNull Boolean dndEnabled,

        @Schema(description = "방해금지 시작 시각 (0~23시, 포함)", example = "22")
        @NotNull
        @Min(value = DoNotDisturb.MIN_HOUR, message = HOUR_MESSAGE)
        @Max(value = DoNotDisturb.MAX_HOUR, message = HOUR_MESSAGE)
        Integer dndStartHour,

        @Schema(description = "방해금지 끝 시각 (0~23시, 미포함). 시작보다 작으면 자정을 넘는 구간", example = "8")
        @NotNull
        @Min(value = DoNotDisturb.MIN_HOUR, message = HOUR_MESSAGE)
        @Max(value = DoNotDisturb.MAX_HOUR, message = HOUR_MESSAGE)
        Integer dndEndHour
) {

    private static final String HOUR_MESSAGE = "시각은 0~23 사이의 정수여야 합니다.";

    /** 켜져 있을 때 시작과 끝이 같으면 구간을 정할 수 없다. (꺼져 있으면 값만 보관) */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "방해금지를 켜려면 시작 시각과 끝 시각이 달라야 합니다.")
    public boolean isDndHoursDistinct() {
        return !Boolean.TRUE.equals(dndEnabled) || dndStartHour == null || !dndStartHour.equals(dndEndHour);
    }

    /** 검증(@Valid)을 통과한 뒤에만 호출한다. */
    public NotificationPreferences toPreferences() {
        return new NotificationPreferences(push, chat, activity, favoriteSpace, marketing,
                DoNotDisturb.of(dndEnabled, dndStartHour, dndEndHour));
    }
}
