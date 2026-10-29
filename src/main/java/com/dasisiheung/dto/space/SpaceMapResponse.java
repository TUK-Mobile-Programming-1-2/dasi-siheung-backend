package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 지도 마커 목록.
 *
 * @param truncated 영역 안 공간이 상한보다 많아 일부만 내려갔는지 — true 면 지도를 확대하라고 안내한다
 */
public record SpaceMapResponse(
        @Schema(description = "마커 (최근 공개 순, 최대 300개)") List<Marker> markers,
        @Schema(description = "상한을 넘어 일부만 보여주는지", example = "false") boolean truncated
) {

    public record Marker(
            @Schema(description = "공간 ID", example = "1") Long id,
            @Schema(description = "공간 이름") String title,
            @Schema(description = "서비스 지역", example = "JEONGWANG") Region region,
            @Schema(description = "1일 이용료(원)", example = "50000") int dailyPrice,
            @Schema(description = "위도", example = "37.3517") double latitude,
            @Schema(description = "경도", example = "126.7428") double longitude
    ) {
    }
}
