package com.dasisiheung.dto.space;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

/** 사용 가능 날짜(캘린더)를 통째로 바꾼다. 빈 목록이면 모든 날짜를 닫는다. */
public record AvailableDatesRequest(
        @Schema(description = "사용 가능 날짜 (오늘~1년 이내)", example = "[\"2026-11-01\", \"2026-11-02\"]")
        @NotNull @Size(max = 366) Set<@NotNull LocalDate> dates
) {
}
