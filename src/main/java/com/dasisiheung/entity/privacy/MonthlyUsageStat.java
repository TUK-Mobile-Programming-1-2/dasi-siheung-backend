package com.dasisiheung.entity.privacy;

import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** 월별 · 지역별 · 활동별 확정 이용 통계. 집계 SQL 이 만들고 애플리케이션은 읽기만 한다. */
@Getter
@Entity
@Immutable
@Table(name = "monthly_usage_stat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyUsageStat {

    @EmbeddedId
    private Key key;

    @Column(name = "booking_count", nullable = false)
    private int bookingCount;

    @Column(name = "booked_days", nullable = false)
    private int bookedDays;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Getter
    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Key implements Serializable {

        /** 그 달 1일. */
        @Column(name = "stat_month", nullable = false)
        private LocalDate statMonth;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Region region;

        @Enumerated(EnumType.STRING)
        @Column(name = "activity_type", nullable = false, length = 30)
        private ActivityType activityType;

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && statMonth.equals(other.statMonth) && region == other.region
                    && activityType == other.activityType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(statMonth, region, activityType);
        }
    }
}
