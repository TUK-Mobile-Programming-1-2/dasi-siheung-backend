package com.dasisiheung.entity.faq;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 자주 묻는 질문 분류. 선택지 조회 API 의 faqCategories 로 내려간다. */
@Getter
@RequiredArgsConstructor
public enum FaqCategory implements LabeledEnum {

    ACCOUNT("회원 · 계정"),
    APPLICATION("신청 · 예약"),
    SPACE("공간 등록 · 관리"),
    COMMUNITY("커뮤니티 · 신고"),
    ETC("기타");

    private final String label;
}
