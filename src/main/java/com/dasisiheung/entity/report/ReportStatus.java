package com.dasisiheung.entity.report;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus implements LabeledEnum {

    PENDING("처리 대기"),
    RESOLVED("조치 완료"),
    DISMISSED("기각");

    private final String label;
}
