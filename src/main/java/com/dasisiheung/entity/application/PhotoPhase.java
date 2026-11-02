package com.dasisiheung.entity.application;

import com.dasisiheung.entity.LabeledEnum;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 활동 사진 시점. 올릴 수 있는 기간이 다르다.
 * <ul>
 *   <li>BEFORE: 이용 시작 하루 전 ~ 종료일</li>
 *   <li>AFTER: 이용 시작일 ~ 종료 7일 뒤</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum PhotoPhase implements LabeledEnum {

    BEFORE("이용 전", 1, 0),
    AFTER("이용 후 (원상 복구)", 0, 7);

    private final String label;
    private final int daysBeforeStart;
    private final int daysAfterEnd;

    public boolean isOpen(LocalDate startDate, LocalDate endDate, LocalDate today) {
        return !today.isBefore(startDate.minusDays(daysBeforeStart)) && !today.isAfter(endDate.plusDays(daysAfterEnd));
    }
}
