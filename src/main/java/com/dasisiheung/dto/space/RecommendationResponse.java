package com.dasisiheung.dto.space;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 추천 공간 한 곳. */
public record RecommendationResponse(
        @Schema(description = "공간 카드") SpaceSummaryResponse space,
        @Schema(description = "추천 점수 (0~100)", example = "78.5") double score,
        @Schema(description = "추천 이유", example = "[\"브랜드 홍보에 맞는 상권 (카페거리)\", \"평점 4.5 (후기 4개)\"]") List<String> reasons
) {
}
