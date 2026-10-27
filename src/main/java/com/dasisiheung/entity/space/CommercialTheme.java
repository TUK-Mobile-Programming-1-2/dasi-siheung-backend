package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 같은·비슷한 업종이 모인 상권 특성. 목적 기반 추천과 검색에 쓴다. */
@Getter
@RequiredArgsConstructor
public enum CommercialTheme implements LabeledEnum {

    FOOD_ALLEY("먹자골목"),
    CHICKEN_ALLEY("치킨골목"),
    CAFE_STREET("카페거리"),
    CAMPUS_TOWN("대학가"),
    WATERFRONT("수변 관광지"),
    RESIDENTIAL("주거 밀집 상권"),
    INDUSTRIAL("산업단지 배후 상권");

    private final String label;
}
