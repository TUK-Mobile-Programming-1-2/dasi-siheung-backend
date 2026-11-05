package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 공간 제공자가 보는 내 공간 상세 (검수 상태·사유 포함). */
public record ProviderSpaceResponse(
        @Schema(description = "공간 ID", example = "1") Long id,
        @Schema(description = "공개 상태", example = "PENDING_REVIEW") SpaceStatus status,
        @Schema(description = "가장 최근 검수 사유 (없으면 null)", example = "건물·호실이 증빙과 다릅니다.") String reviewComment,
        @Schema(description = "가장 최근 검수 시각 (없으면 null)") LocalDateTime reviewedAt,
        @Schema(description = "서비스 지역 (좌표로 판정)", example = "JEONGWANG") Region region,
        @Schema(description = "공간 이름") String title,
        @Schema(description = "공간 소개") String description,
        @Schema(description = "이용 수칙") String rules,
        @Schema(description = "도로명 주소") String address,
        @Schema(description = "건물 이름") String buildingName,
        @Schema(description = "호실") String unitNumber,
        @Schema(description = "위도", example = "37.3517") double latitude,
        @Schema(description = "경도", example = "126.7428") double longitude,
        @Schema(description = "전용 면적(㎡)", example = "33.06") BigDecimal areaSqm,
        @Schema(description = "건축물 용도") BuildingUse buildingUse,
        @Schema(description = "상권 테마") CommercialTheme commercialTheme,
        @Schema(description = "1일 이용료(원)", example = "50000") int dailyPrice,
        @Schema(description = "보유 시설") List<Facility> facilities,
        @Schema(description = "허용 활동") List<ActivityType> activityTypes,
        @Schema(description = "최대 수용 인원 (정하지 않았으면 null)", example = "10") Integer maxCapacity,
        @Schema(description = "빌려주는 비품 (입력 순서대로)") List<EquipmentItem> equipment,
        @Schema(description = "가격 협의 가능 여부", example = "true") boolean priceNegotiable,
        @Schema(description = "사용 가능 날짜 (오름차순)") List<LocalDate> availableDates,
        @Schema(description = "사진 (순서대로, 첫 번째가 대표)") List<SpacePhotoResponse> photos,
        @Schema(description = "권한 증빙 (임시 주소 포함)") List<SpaceProofResponse> proofs,
        @Schema(description = "등록 시각") LocalDateTime createdAt,
        @Schema(description = "마지막 수정 시각") LocalDateTime updatedAt
) {

    public static ProviderSpaceResponse of(Space space, List<SpacePhotoResponse> photos,
                                           List<SpaceProofResponse> proofs) {
        return new ProviderSpaceResponse(space.getId(), space.getStatus(), space.getReviewComment(),
                space.getReviewedAt(), space.getRegion(), space.getTitle(), space.getDescription(), space.getRules(),
                space.getAddress(), space.getBuildingName(), space.getUnitNumber(), space.latitude(),
                space.longitude(), space.getAreaSqm(), space.getBuildingUse(), space.getCommercialTheme(),
                space.getDailyPrice(), SpaceResponses.sorted(space.getFacilities()),
                SpaceResponses.sorted(space.getActivityTypes()), space.getMaxCapacity(),
                SpaceResponses.equipment(space), space.isPriceNegotiable(),
                SpaceResponses.sorted(space.getAvailableDates()),
                photos, proofs, space.getCreatedAt(), space.getUpdatedAt());
    }
}
