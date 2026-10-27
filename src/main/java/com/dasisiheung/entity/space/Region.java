package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 서비스 지역. 공간 좌표가 어느 지역 경계(service_region 테이블) 안에 있는지로 서버가 정한다. */
@Getter
@RequiredArgsConstructor
public enum Region implements LabeledEnum {

    JEONGWANG("정왕동"),
    GEOBUKSEOM("거북섬"),
    BAEGOT("배곧");

    private final String label;
}
