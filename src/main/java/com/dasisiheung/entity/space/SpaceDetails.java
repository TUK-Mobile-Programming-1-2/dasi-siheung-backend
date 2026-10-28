package com.dasisiheung.entity.space;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import org.locationtech.jts.geom.Point;

/**
 * 공간 제공자가 입력하는 공간 정보. 등록·수정에 함께 쓴다.
 *
 * @param address      도로명 주소
 * @param buildingName 건물 이름 (선택)
 * @param unitNumber   호실 (선택)
 * @param location     좌표 (앱의 지도 SDK 로 주소를 변환한 값)
 * @param dailyPrice   1일 이용료(원)
 */
public record SpaceDetails(
        String title,
        String description,
        String rules,
        String address,
        String buildingName,
        String unitNumber,
        Point location,
        BigDecimal areaSqm,
        BuildingUse buildingUse,
        CommercialTheme commercialTheme,
        int dailyPrice,
        Set<Facility> facilities,
        Set<ActivityType> activityTypes
) {

    public SpaceDetails {
        facilities = facilities == null ? Set.of() : Set.copyOf(facilities);
        activityTypes = activityTypes == null ? Set.of() : Set.copyOf(activityTypes);
    }

    /**
     * 관리자 검수 항목(주소 · 건물·호실 · 면적 · 용도)이 달라졌는지.
     * 좌표는 주소에 따라오므로 따로 보지 않는다.
     */
    public boolean differsInReviewedFields(SpaceDetails other) {
        return !Objects.equals(address, other.address)
                || !Objects.equals(buildingName, other.buildingName)
                || !Objects.equals(unitNumber, other.unitNumber)
                || areaSqm.compareTo(other.areaSqm) != 0
                || buildingUse != other.buildingUse;
    }
}
