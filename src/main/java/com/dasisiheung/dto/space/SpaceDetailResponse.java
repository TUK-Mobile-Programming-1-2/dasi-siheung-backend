package com.dasisiheung.dto.space;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 이용자가 보는 공간 상세. 공개(PUBLISHED) 공간만 내려가며 검수 정보·증빙·제공자 연락처는 없다.
 */
public record SpaceDetailResponse(
        @Schema(description = "공간 ID", example = "1") Long id,
        @Schema(description = "서비스 지역", example = "JEONGWANG") Region region,
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
        @Schema(description = "오늘 이후 사용 가능 날짜 (오름차순)") List<LocalDate> availableDates,
        @Schema(description = "사진 (순서대로, 첫 번째가 대표)") List<SpacePhotoResponse> photos,
        @Schema(description = "공간 제공자") ProviderProfile provider,
        @Schema(description = "찜 수", example = "12") int favoriteCount,
        @Schema(description = "내가 찜했는지 (비로그인이면 false)", example = "false") boolean favorited
) {

    /**
     * @param availableDates 오늘 이후로 거른 날짜
     * @param favorited      로그인 회원이 찜했는지
     */
    public static SpaceDetailResponse of(Space space, List<LocalDate> availableDates, List<SpacePhotoResponse> photos,
                                         boolean favorited) {
        return new SpaceDetailResponse(space.getId(), space.getRegion(), space.getTitle(), space.getDescription(),
                space.getRules(), space.getAddress(), space.getBuildingName(), space.getUnitNumber(),
                space.latitude(), space.longitude(), space.getAreaSqm(), space.getBuildingUse(),
                space.getCommercialTheme(), space.getDailyPrice(), SpaceResponses.sorted(space.getFacilities()),
                SpaceResponses.sorted(space.getActivityTypes()), availableDates, photos,
                ProviderProfile.from(space.getProvider()), space.getFavoriteCount(), favorited);
    }

    /** 공개 프로필. 연락처는 1:1 소통으로만 주고받는다. */
    public record ProviderProfile(
            @Schema(description = "제공자 회원 ID", example = "2") Long id,
            @Schema(description = "제공자 이름", example = "김제공") String name
    ) {

        public static ProviderProfile from(Member provider) {
            return new ProviderProfile(provider.getId(), provider.getName());
        }
    }
}
