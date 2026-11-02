package com.dasisiheung.entity.report;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason implements LabeledEnum {

    FRAUD("사기 · 권한 없는 공간"),
    FALSE_INFORMATION("허위 정보"),
    ABUSIVE("욕설 · 비방"),
    SPAM("광고 · 도배"),
    INAPPROPRIATE("부적절한 내용"),
    OTHER("기타");

    private final String label;
}
