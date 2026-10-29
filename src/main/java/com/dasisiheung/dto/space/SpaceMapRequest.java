package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.ActivityType;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 지도 화면 영역. 지도 SDK 의 getBounds() 남서(sw)·북동(ne) 꼭짓점을 그대로 보낸다.
 */
public record SpaceMapRequest(
        @Schema(description = "남서쪽 위도", example = "37.30")
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double swLat,

        @Schema(description = "남서쪽 경도", example = "126.65")
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double swLng,

        @Schema(description = "북동쪽 위도", example = "37.40")
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double neLat,

        @Schema(description = "북동쪽 경도", example = "126.80")
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double neLng,

        @Schema(description = "하고 싶은 활동 (선택)", example = "POPUP_STORE")
        ActivityType activityType,

        @Schema(description = "1일 이용료 상한(원, 선택)", example = "80000")
        @Min(0) Integer maxPrice
) {

    @Parameter(hidden = true)
    @AssertTrue(message = "남서쪽 꼭짓점이 북동쪽보다 남쪽·서쪽에 있어야 합니다.")
    public boolean isBoundsValid() {
        if (swLat == null || swLng == null || neLat == null || neLng == null) {
            return true; // @NotNull 이 따로 알려 준다
        }
        return swLat < neLat && swLng < neLng;
    }
}
