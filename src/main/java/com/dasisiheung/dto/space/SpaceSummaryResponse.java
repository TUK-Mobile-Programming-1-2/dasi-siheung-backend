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
        @Schema(description = "최대 수용 인원 (정하지 않았으면 null)", example = "10") Integer maxCapacity,
        @Schema(description = "가격 협의 가능 여부", example = "true") boolean priceNegotiable,
        @Schema(description = "위도", example = "37.3517") double latitude,
        @Schema(description = "경도", example = "126.7428") double longitude,
        @Schema(description = "찜 수", example = "12") int favoriteCount,
        @Schema(description = "후기 수", example = "4") int reviewCount,
        @Schema(description = "평균 평점 (후기 없으면 null)", example = "4.50") BigDecimal ratingAverage
) {

    public static SpaceSummaryResponse of(Space space, String thumbnailUrl) {
        return new SpaceSummaryResponse(space.getId(), space.getTitle(), space.getRegion(), space.getAddress(),
                thumbnailUrl, space.getDailyPrice(), space.getAreaSqm(), space.getCommercialTheme(),
                SpaceResponses.sorted(space.getActivityTypes()), space.getMaxCapacity(), space.isPriceNegotiable(),
                space.latitude(), space.longitude(),
                space.getFavoriteCount(), space.getReviewCount(), space.getRatingAverage());
    }
}
