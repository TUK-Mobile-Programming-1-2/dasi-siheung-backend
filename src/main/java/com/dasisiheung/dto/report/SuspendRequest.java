package com.dasisiheung.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 관리자의 직접 이용 정지. */
public record SuspendRequest(
        @Schema(description = "정지 일수 (1~365)", example = "7")
        @NotNull @Min(1) @Max(365) Integer days
) {
}
