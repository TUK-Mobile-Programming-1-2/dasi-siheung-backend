package com.dasisiheung.entity.application;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 신청의 어느 쪽인지 (가격을 제안한 쪽). */
@Getter
@RequiredArgsConstructor
public enum OfferSide implements LabeledEnum {

    APPLICANT("신청자"),
    PROVIDER("공간 제공자");

    private final String label;

    /** 이 쪽이 제안하면 상대 답변을 기다리는 상태. */
    PriceNegotiationStatus waitingForCounterpart() {
        return this == APPLICANT ? PriceNegotiationStatus.WAITING_PROVIDER : PriceNegotiationStatus.WAITING_APPLICANT;
    }

    /** 이 쪽이 답변할 차례인 상태. */
    PriceNegotiationStatus waitingForMe() {
        return this == APPLICANT ? PriceNegotiationStatus.WAITING_APPLICANT : PriceNegotiationStatus.WAITING_PROVIDER;
    }
}
