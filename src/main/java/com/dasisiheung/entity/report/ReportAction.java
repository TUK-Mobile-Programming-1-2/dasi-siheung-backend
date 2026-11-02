package com.dasisiheung.entity.report;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 관리자 신고 처리. */
@Getter
@RequiredArgsConstructor
public enum ReportAction implements LabeledEnum {

    DISMISS("기각", false, false),
    HIDE("대상 숨김", true, false),
    SUSPEND("작성자 · 제공자 이용 정지", false, true),
    HIDE_AND_SUSPEND("숨김 + 이용 정지", true, true);

    private final String label;
    private final boolean hidesTarget;
    private final boolean suspendsOwner;

    public ReportStatus resultStatus() {
        return this == DISMISS ? ReportStatus.DISMISSED : ReportStatus.RESOLVED;
    }
}
