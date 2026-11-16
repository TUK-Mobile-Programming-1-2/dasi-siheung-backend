package com.dasisiheung.entity.inquiry;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 1:1 문의 상태. 답변은 한 번만 등록하므로 답변 완료에서 다시 대기로 돌아가지 않는다. */
@Getter
@RequiredArgsConstructor
public enum InquiryStatus implements LabeledEnum {

    WAITING("답변 대기"),
    ANSWERED("답변 완료");

    private final String label;
}
