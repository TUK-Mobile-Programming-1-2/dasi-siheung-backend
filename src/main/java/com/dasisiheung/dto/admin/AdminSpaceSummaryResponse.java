package com.dasisiheung.dto.admin;

import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 관리자 검수 대기 목록 · 전체 공간 현황 항목. */
public record AdminSpaceSummaryResponse(
        @Schema(description = "공간 ID", example = "1") Long id,
        @Schema(description = "공간 이름") String title,
        @Schema(description = "서비스 지역") Region region,
        @Schema(description = "주소") String address,
        @Schema(description = "공개 상태") SpaceStatus status,
        @Schema(description = "공간 제공자 이름") String providerName,
        @Schema(description = "증빙 수 (0 이면 공개 승인 불가)", example = "1") int proofCount,
        @Schema(description = "사진 수", example = "3") int photoCount,
        @Schema(description = "등록 시각") LocalDateTime createdAt,
        @Schema(description = "마지막 수정 시각 (검수 대기 순서 기준)") LocalDateTime updatedAt
) {

    public static AdminSpaceSummaryResponse from(Space space) {
        return new AdminSpaceSummaryResponse(space.getId(), space.getTitle(), space.getRegion(), space.getAddress(),
                space.getStatus(), space.getProvider().getName(), space.getProofs().size(), space.getPhotos().size(),
                space.getCreatedAt(), space.getUpdatedAt());
    }
}
