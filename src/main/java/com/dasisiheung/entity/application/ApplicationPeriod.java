package com.dasisiheung.entity.application;

import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * 이용 기간 (시작일 · 종료일 모두 포함).
 *
 * @throws BusinessException 종료일이 시작일보다 빠르면 INVALID_APPLICATION_PERIOD
 */
public record ApplicationPeriod(LocalDate startDate, LocalDate endDate) {

    /** 한 번에 신청할 수 있는 최대 일수. */
    public static final int MAX_DAYS = 30;

    public ApplicationPeriod {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_PERIOD);
        }
    }

    public int days() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public Stream<LocalDate> dates() {
        return startDate.datesUntil(endDate.plusDays(1));
    }

    public boolean overlaps(ApplicationPeriod other) {
        return !startDate.isAfter(other.endDate) && !other.startDate.isAfter(endDate);
    }

    public boolean isCoveredBy(Collection<LocalDate> availableDates) {
        return dates().allMatch(availableDates::contains);
    }

    /** 오늘 이후에 시작하고 최대 일수 안인지. */
    public void validateApplicable(LocalDate today) {
        if (startDate.isBefore(today) || days() > MAX_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_PERIOD);
        }
    }
}
