package com.dasisiheung.dto.admin;

import com.dasisiheung.entity.space.CommercialTheme;
import io.swagger.v3.oas.annotations.media.Schema;

/** 관리자 상권 테마 보정. null 이면 테마를 비운다. */
public record ThemeCorrectionRequest(
        @Schema(description = "상권 테마 (선택지 commercialThemes, null 이면 비움)", example = "CAFE_STREET")
        CommercialTheme commercialTheme
) {
}
