package com.dasisiheung.entity.application;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 공간 이용 신청 상태. */
@Getter
@RequiredArgsConstructor
public enum ApplicationStatus implements LabeledEnum {

    PENDING("검토 중"),
    APPROVED("예약 확정"),
    REJECTED("거절"),
    CANCELLED("취소"),
    /** 같은 기간의 다른 신청이 확정되어 자동으로 마감됨. */
    CLOSED("마감");

    private final String label;

    public boolean isFinished() {
        return this != PENDING && this != APPROVED;
    }
}
