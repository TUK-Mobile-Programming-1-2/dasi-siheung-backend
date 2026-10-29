package com.dasisiheung.repository;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * 공간 검색 조건 조각. 서비스가 필요한 조건만 골라 {@link Specification#allOf} 로 묶는다.
 * 집합 컬럼(시설·활동)은 join 대신 member of 로 검사해 결과가 중복되지 않는다.
 */
public final class SpaceSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private SpaceSpecifications() {
    }

    public static Specification<Space> hasStatus(SpaceStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Space> inRegion(Region region) {
        return (root, query, cb) -> cb.equal(root.get("region"), region);
    }

    public static Specification<Space> allowsActivity(ActivityType activityType) {
        return (root, query, cb) -> cb.isMember(activityType, root.<Collection<ActivityType>>get("activityTypes"));
    }

    public static Specification<Space> hasAllFacilities(Collection<Facility> facilities) {
        return (root, query, cb) -> cb.and(facilities.stream()
                .map(facility -> cb.isMember(facility, root.<Collection<Facility>>get("facilities")))
                .toArray(Predicate[]::new));
    }

    public static Specification<Space> themeIn(Collection<CommercialTheme> themes) {
        return (root, query, cb) -> root.get("commercialTheme").in(themes);
    }

    public static Specification<Space> priceAtLeast(int minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dailyPrice"), minPrice);
    }

    public static Specification<Space> priceAtMost(int maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dailyPrice"), maxPrice);
    }

    /** 공간 이름 또는 주소에 검색어가 들어 있다. (대소문자 무시, % _ 는 글자 그대로) */
    public static Specification<Space> containsKeyword(String keyword) {
        String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("address")), pattern, LIKE_ESCAPE));
    }

    /** 기간의 모든 날짜가 사용 가능 날짜에 들어 있다. (기간 안의 가능 날짜 수 = 기간 일수) */
    public static Specification<Space> availableThroughout(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return (root, query, cb) -> {
            Subquery<Long> availableDays = query.subquery(Long.class);
            Root<Space> sub = availableDays.from(Space.class);
            Join<Space, LocalDate> date = sub.join("availableDates");
            availableDays.select(cb.count(date))
                    .where(cb.equal(sub, root), cb.between(date, startDate, endDate));
            return cb.equal(availableDays, days);
        };
    }

    /** 기간과 겹치는 확정 예약이 없다. */
    public static Specification<Space> notBookedBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            Subquery<Long> booked = query.subquery(Long.class);
            Root<Application> application = booked.from(Application.class);
            booked.select(application.get("id"))
                    .where(cb.equal(application.get("space"), root),
                            cb.equal(application.get("status"), ApplicationStatus.APPROVED),
                            cb.lessThanOrEqualTo(application.<LocalDate>get("startDate"), endDate),
                            cb.greaterThanOrEqualTo(application.<LocalDate>get("endDate"), startDate));
            return cb.not(cb.exists(booked));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
