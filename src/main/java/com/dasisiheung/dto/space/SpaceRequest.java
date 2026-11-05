package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.BuildingUse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceDetails;
import com.dasisiheung.util.GeoPoints;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 공간 등록·수정 입력. 좌표는 앱의 지도 SDK 로 주소를 변환한 값을 보낸다.
 * 지역(정왕동·거북섬·배곧)은 서버가 좌표로 판정한다.
 */
public record SpaceRequest(
        @Schema(description = "공간 이름", example = "정왕역 앞 1층 상가")
        @NotBlank @Size(max = 60) String title,

        @Schema(description = "공간 소개", example = "유동 인구가 많은 역 앞 코너 상가입니다.")
        @NotBlank @Size(max = 2000) String description,

        @Schema(description = "이용 수칙 (선택)", example = "21시 이후 소음 금지, 퇴실 시 원상 복구")
        @Size(max = 2000) String rules,

        @Schema(description = "도로명 주소", example = "경기도 시흥시 마유로 418")
        @NotBlank @Size(max = 200) String address,

        @Schema(description = "건물 이름 (선택)", example = "정왕프라자")
        @Size(max = 60) String buildingName,

        @Schema(description = "호실 (선택)", example = "101호")
        @Size(max = 30) String unitNumber,

        @Schema(description = "위도", example = "37.3517")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,

        @Schema(description = "경도", example = "126.7428")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,

        @Schema(description = "전용 면적(㎡)", example = "33.06")
        @NotNull @DecimalMin("1.00") @Digits(integer = 6, fraction = 2) BigDecimal areaSqm,

        @Schema(description = "건축물 용도 (선택지 buildingUses)", example = "NEIGHBORHOOD_1")
        @NotNull BuildingUse buildingUse,

        @Schema(description = "상권 테마 (선택지 commercialThemes, 선택)", example = "FOOD_ALLEY")
        CommercialTheme commercialTheme,

        @Schema(description = "1일 이용료(원)", example = "50000")
        @NotNull @PositiveOrZero @Max(10_000_000) Integer dailyPrice,

        @Schema(description = "보유 시설 (선택지 facilities)", example = "[\"WIFI\", \"ELECTRICITY\"]")
        @Size(max = 20) Set<Facility> facilities,

        @Schema(description = "허용 활동 (선택지 activityTypes, 1개 이상)", example = "[\"POPUP_STORE\", \"EXHIBITION\"]")
        @NotEmpty Set<ActivityType> activityTypes,

        @Schema(description = "사용 가능 날짜 (오늘~1년 이내). 수정할 때 보내지 않으면(null) 기존 캘린더를 유지한다",
                example = "[\"2026-11-01\", \"2026-11-02\"]")
        @Size(max = 366) Set<LocalDate> availableDates,

        @Schema(description = "최대 수용 인원 (1~500, 선택). 정하면 이보다 많은 운영 인원의 신청은 받지 않는다", example = "10")
        @Min(1) @Max(Space.MAX_CAPACITY) Integer maxCapacity,

        @Schema(description = "빌려주는 비품 (이름 · 수량, 최대 20줄, 선택). 수정할 때는 보낸 목록으로 통째로 바꾼다")
        @Size(max = Space.MAX_EQUIPMENT) List<@Valid EquipmentItem> equipment,

        @Schema(description = "가격 협의 가능 여부 (선택, 기본 true). false 면 이용자는 정가로만 신청한다", example = "true")
        Boolean priceNegotiable
) {

    public SpaceDetails toDetails() {
        List<EquipmentItem> items = equipment == null ? List.of() : equipment;
        return new SpaceDetails(title, description, rules, address, buildingName, unitNumber,
                GeoPoints.of(latitude, longitude), areaSqm, buildingUse, commercialTheme, dailyPrice,
                facilities, activityTypes, maxCapacity, items.stream().map(EquipmentItem::toEntity).toList(),
                priceNegotiable == null || priceNegotiable);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "비품 이름이 겹칩니다. 같은 비품은 수량으로 합쳐 주세요.")
    public boolean isEquipmentNamesUnique() {
        if (equipment == null) {
            return true;
        }
        Set<String> names = new HashSet<>();
        return equipment.stream()
                .filter(item -> item != null && item.name() != null)
                .allMatch(item -> names.add(item.name().strip().toLowerCase(Locale.ROOT)));
    }
}
