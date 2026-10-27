package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공간 보유 시설. */
@Getter
@RequiredArgsConstructor
public enum Facility implements LabeledEnum {

    WIFI("와이파이"),
    ELECTRICITY("전기"),
    WATER("수도"),
    RESTROOM("화장실"),
    AIR_CONDITIONER("냉방"),
    HEATING("난방"),
    PARKING("주차"),
    TABLE_CHAIR("테이블·의자"),
    DISPLAY_SHELF("진열대"),
    SINK("싱크대"),
    SIGNBOARD("간판 설치");

    private final String label;
}
