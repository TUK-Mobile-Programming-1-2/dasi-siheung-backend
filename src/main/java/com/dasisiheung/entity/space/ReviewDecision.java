package com.dasisiheung.entity.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 관리자 검수 결과와, 결과에 따라 바뀌는 공간 상태. */
@Getter
@RequiredArgsConstructor
public enum ReviewDecision implements LabeledEnum {

    APPROVE("공개 승인", SpaceStatus.PUBLISHED),
    REQUEST_REVISION("수정 요청", SpaceStatus.REVISION_REQUESTED),
    HOLD("공개 보류", SpaceStatus.ON_HOLD);

    private final String label;
    private final SpaceStatus resultStatus;
}
