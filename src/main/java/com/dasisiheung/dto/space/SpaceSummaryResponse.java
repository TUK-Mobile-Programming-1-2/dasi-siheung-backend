package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** 검색 결과 · 목록 카드. */
public record SpaceSummaryResponse(
        @Schema(description = "공간 ID", example = "1") Long id,
        @Schema(description = "공간 이름") String title,
        @Schema(description = "서비스 지역", example = "JEONGWANG") Region region,
        @Schema(description = "도로명 주소") String address,
        @Schema(description = "대표 사진 주소 (없으면 null)") String thumbnailUrl,
        @Schema(description = "1일 이용료(원)", example = "50000") int dailyPrice,
        @Schema(description = "전용 면적(㎡)", example = "33.06") BigDecimal areaSqm,
        @Schema(description = "상권 테마") CommercialTheme commercialTheme,
        @Schema(description = "허용 활동") List<ActivityType> activityTypes,
        @Schema(description = "위도", example = "37.3517") double latitude,
        @Schema(description = "경도", example = "126.7428") double longitude,
        @Schema(description = "찜 수", example = "12") int favoriteCount
) {

    public static SpaceSummaryResponse of(Space space, String thumbnailUrl) {
        return new SpaceSummaryResponse(space.getId(), space.getTitle(), space.getRegion(), space.getAddress(),
                thumbnailUrl, space.getDailyPrice(), space.getAreaSqm(), space.getCommercialTheme(),
                SpaceResponses.sorted(space.getActivityTypes()), space.latitude(), space.longitude(),
                space.getFavoriteCount());
    }
}
