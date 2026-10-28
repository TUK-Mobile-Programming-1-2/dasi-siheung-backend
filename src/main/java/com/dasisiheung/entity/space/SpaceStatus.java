package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공간 공개 상태.
 * <pre>
 * 등록 ─▶ PENDING_REVIEW ──승인──▶ PUBLISHED ⇄ PAUSED (제공자가 일시 중단·재개)
 *              │  ▲                    │
 *        수정 요청│  │다시 제출           └─ 검수 항목(주소·건물·면적·용도·증빙) 수정 시 PENDING_REVIEW
 *              ▼  │
 *   REVISION_REQUESTED / ON_HOLD(증빙 부족)
 * HIDDEN: 신고 처리로 관리자가 숨김
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum SpaceStatus implements LabeledEnum {

    PENDING_REVIEW("검수 대기"),
    REVISION_REQUESTED("수정 요청"),
    ON_HOLD("공개 보류"),
    PUBLISHED("공개"),
    PAUSED("일시 중단"),
    HIDDEN("숨김");

    private final String label;

    /** 이용자에게 보이는 상태 */
    public boolean isVisible() {
        return this == PUBLISHED;
    }
}
