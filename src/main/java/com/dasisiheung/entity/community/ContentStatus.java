package com.dasisiheung.entity.community;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 게시글 · 댓글 노출 상태. 신고 처리로 관리자가 숨길 수 있다. */
@Getter
@RequiredArgsConstructor
public enum ContentStatus implements LabeledEnum {

    VISIBLE("보임"),
    HIDDEN("숨김 (신고 처리)");

    private final String label;
}
