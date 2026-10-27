package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 건축물대장 기준 용도. 관리자가 제출 증빙과 일치하는지 검수한다. */
@Getter
@RequiredArgsConstructor
public enum BuildingUse implements LabeledEnum {

    NEIGHBORHOOD_1("제1종 근린생활시설"),
    NEIGHBORHOOD_2("제2종 근린생활시설"),
    SALES("판매시설"),
    BUSINESS("업무시설"),
    CULTURE("문화 및 집회시설"),
    ETC("기타");

    private final String label;
}
