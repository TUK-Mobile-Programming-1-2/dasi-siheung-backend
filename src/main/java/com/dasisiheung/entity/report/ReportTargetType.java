package com.dasisiheung.entity.report;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 신고 대상 종류. 회원은 숨길 수 없고 이용 정지만 할 수 있다. */
@Getter
@RequiredArgsConstructor
public enum ReportTargetType implements LabeledEnum {

    SPACE("공간", true),
    POST("게시글", true),
    COMMENT("댓글", true),
    MEMBER("회원", false);

    private final String label;
    private final boolean hideable;
}
