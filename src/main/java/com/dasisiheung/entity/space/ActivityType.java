package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공간에서 허용하는 활동이자, 이용 신청서의 활동 종류. */
@Getter
@RequiredArgsConstructor
public enum ActivityType implements LabeledEnum {

    POPUP_STORE("팝업 판매"),
    EXHIBITION("전시"),
    EXPERIENCE("체험 행사"),
    CLASS("클래스·워크숍"),
    MEETING("모임·네트워킹");

    private final String label;
}
