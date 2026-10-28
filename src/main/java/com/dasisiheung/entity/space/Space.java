package com.dasisiheung.entity.space;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Getter
@Entity
@Table(name = "space")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space extends BaseTimeEntity {

    /** 가능 날짜는 오늘부터 이 기간 안에서만 지정할 수 있다. */
    public static final int AVAILABLE_DATE_WINDOW_DAYS = 365;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Member provider;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 2000)
    private String rules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Region region;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(name = "building_name", length = 60)
    private String buildingName;

    @Column(name = "unit_number", length = 30)
    private String unitNumber;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "area_sqm", nullable = false, precision = 8, scale = 2)
    private BigDecimal areaSqm;

    @Enumerated(EnumType.STRING)
    @Column(name = "building_use", nullable = false, length = 30)
    private BuildingUse buildingUse;

    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_theme", length = 30)
    private CommercialTheme commercialTheme;

    @Column(name = "daily_price", nullable = false)
    private int dailyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpaceStatus status;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "space_facility", joinColumns = @JoinColumn(name = "space_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "facility", nullable = false, length = 30)
    private Set<Facility> facilities = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "space_activity", joinColumns = @JoinColumn(name = "space_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private Set<ActivityType> activityTypes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "space_available_date", joinColumns = @JoinColumn(name = "space_id"))
    @Column(name = "available_date", nullable = false)
    private Set<LocalDate> availableDates = new HashSet<>();

    private Space(Member provider, Region region, SpaceDetails details) {
        this.provider = provider;
        this.status = SpaceStatus.PENDING_REVIEW;
        apply(region, details);
    }

    /** 공간 등록. 관리자 검수 대기 상태로 시작한다. */
    public static Space register(Member provider, Region region, SpaceDetails details,
                                 Set<LocalDate> availableDates, LocalDate today) {
        Space space = new Space(provider, region, details);
        space.changeAvailableDates(availableDates, today);
        return space;
    }

    public boolean isOwnedBy(Long memberId) {
        return provider.getId().equals(memberId);
    }

    /**
     * 사용 가능 날짜(캘린더)를 통째로 바꾼다.
     *
     * @throws BusinessException 오늘 이전이거나 1년 뒤보다 늦은 날짜가 있으면 INVALID_AVAILABLE_DATE
     */
    public void changeAvailableDates(Set<LocalDate> dates, LocalDate today) {
        Set<LocalDate> newDates = dates == null ? Set.of() : dates;
        LocalDate lastDate = today.plusDays(AVAILABLE_DATE_WINDOW_DAYS);
        boolean outOfWindow = newDates.stream().anyMatch(date -> date.isBefore(today) || date.isAfter(lastDate));
        if (outOfWindow) {
            throw new BusinessException(ErrorCode.INVALID_AVAILABLE_DATE);
        }
        availableDates.clear();
        availableDates.addAll(newDates);
    }

    public double latitude() {
        return location.getY();
    }

    public double longitude() {
        return location.getX();
    }

    private void apply(Region region, SpaceDetails details) {
        this.region = region;
        this.title = details.title().strip();
        this.description = details.description().strip();
        this.rules = blankToNull(details.rules());
        this.address = details.address().strip();
        this.buildingName = blankToNull(details.buildingName());
        this.unitNumber = blankToNull(details.unitNumber());
        this.location = details.location();
        this.areaSqm = details.areaSqm();
        this.buildingUse = details.buildingUse();
        this.commercialTheme = details.commercialTheme();
        this.dailyPrice = details.dailyPrice();
        replace(facilities, details.facilities());
        replace(activityTypes, details.activityTypes());
    }

    protected SpaceDetails currentDetails() {
        return new SpaceDetails(title, description, rules, address, buildingName, unitNumber, location, areaSqm,
                buildingUse, commercialTheme, dailyPrice, facilities, activityTypes);
    }

    private static <T> void replace(Set<T> target, Set<T> values) {
        if (!Objects.equals(target, values)) {
            target.clear();
            target.addAll(values);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
