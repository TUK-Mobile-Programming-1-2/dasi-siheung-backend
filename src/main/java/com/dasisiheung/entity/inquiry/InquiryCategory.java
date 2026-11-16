package com.dasisiheung.entity.inquiry;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 1:1 문의 분류. 선택지 조회 API 의 inquiryCategories 로 내려간다. (앱 화면 순서) */
@Getter
@RequiredArgsConstructor
public enum InquiryCategory implements LabeledEnum {

    APPLICATION("신청 · 예약"),
    PAYMENT("결제 · 환불"),
    ACCOUNT("계정"),
    SPACE("공간 정보"),
    ETC("기타");

    private final String label;
}
