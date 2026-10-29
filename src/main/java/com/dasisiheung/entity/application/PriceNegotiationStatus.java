package com.dasisiheung.entity.application;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 가격 협의 진행 상태. 제안 대기 중에는 받은 쪽만 수락하거나 다시 제안할 수 있다. */
@Getter
@RequiredArgsConstructor
public enum PriceNegotiationStatus implements LabeledEnum {

    NONE("정가 신청"),
    WAITING_PROVIDER("제공자 답변 대기"),
    WAITING_APPLICANT("신청자 답변 대기"),
    AGREED("가격 합의");

    private final String label;

    public boolean isWaiting() {
        return this == WAITING_PROVIDER || this == WAITING_APPLICANT;
    }
}
