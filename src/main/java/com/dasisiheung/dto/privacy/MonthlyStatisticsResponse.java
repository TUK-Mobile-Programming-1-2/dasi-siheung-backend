package com.dasisiheung.dto.privacy;

import com.dasisiheung.entity.privacy.MonthlyUsageStat;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;
import java.util.List;

/** 월별 확정 이용 통계 (개인 식별 정보 없음). 어제까지 이용이 끝난 예약 기준. */
public record MonthlyStatisticsResponse(
        @Schema(description = "조회 시작 월", example = "2026-01") YearMonth from,
        @Schema(description = "조회 끝 월", example = "2026-12") YearMonth to,
        @Schema(description = "확정 예약 수 합계", example = "42") long totalBookings,
        @Schema(description = "이용 일수 합계", example = "96") long totalBookedDays,
        @Schema(description = "합의 금액 합계(원)", example = "4800000") long totalAmount,
        @Schema(description = "월 · 지역 · 활동별 행") List<Row> rows
) {

    public static MonthlyStatisticsResponse of(YearMonth from, YearMonth to, List<MonthlyUsageStat> stats) {
        List<Row> rows = stats.stream().map(Row::from).toList();
        return new MonthlyStatisticsResponse(from, to,
                rows.stream().mapToLong(Row::bookingCount).sum(),
                rows.stream().mapToLong(Row::bookedDays).sum(),
                rows.stream().mapToLong(Row::totalAmount).sum(), rows);
    }

    public record Row(
            @Schema(description = "월", example = "2026-10") YearMonth month,
            @Schema(description = "지역", example = "JEONGWANG") Region region,
            @Schema(description = "활동", example = "POPUP_STORE") ActivityType activityType,
            @Schema(description = "확정 예약 수", example = "4") int bookingCount,
            @Schema(description = "이용 일수", example = "9") int bookedDays,
            @Schema(description = "합의 금액 합계(원)", example = "430000") long totalAmount
    ) {

        static Row from(MonthlyUsageStat stat) {
            return new Row(YearMonth.from(stat.getKey().getStatMonth()), stat.getKey().getRegion(),
                    stat.getKey().getActivityType(), stat.getBookingCount(), stat.getBookedDays(), stat.getTotalAmount());
        }
    }
}
