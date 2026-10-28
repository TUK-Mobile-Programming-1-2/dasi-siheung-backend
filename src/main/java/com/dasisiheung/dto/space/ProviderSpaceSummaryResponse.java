package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 내 공간 목록 항목 — 검수 상태와 사유를 함께 보여준다. */
public record ProviderSpaceSummaryResponse(
        @Schema(description = "공간 ID", example = "1") Long id,
        @Schema(description = "공간 이름") String title,
        @Schema(description = "서비스 지역", example = "JEONGWANG") Region region,
        @Schema(description = "공개 상태", example = "REVISION_REQUESTED") SpaceStatus status,
        @Schema(description = "가장 최근 검수 사유") String reviewComment,
        @Schema(description = "대표 사진 주소 (없으면 null)") String thumbnailUrl,
        @Schema(description = "1일 이용료(원)", example = "50000") int dailyPrice,
        @Schema(description = "사진 수", example = "3") int photoCount,
        @Schema(description = "증빙 수", example = "1") int proofCount,
        @Schema(description = "사용 가능 날짜 수", example = "14") int availableDateCount,
        @Schema(description = "마지막 수정 시각") LocalDateTime updatedAt
) {

    public static ProviderSpaceSummaryResponse of(Space space, String thumbnailUrl) {
        return new ProviderSpaceSummaryResponse(space.getId(), space.getTitle(), space.getRegion(), space.getStatus(),
                space.getReviewComment(), thumbnailUrl, space.getDailyPrice(), space.getPhotos().size(),
                space.getProofs().size(), space.getAvailableDates().size(), space.getUpdatedAt());
    }
}
