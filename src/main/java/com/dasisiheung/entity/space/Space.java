package com.dasisiheung.entity.space;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
    public static final int MAX_PHOTOS = 10;
    public static final int MAX_PROOFS = 5;

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

    /**
     * 찜 수. 동시에 찜해도 빠지지 않도록 저장소의 UPDATE 한 문장으로만 바꾸고,
     * 엔티티 저장(공간 수정 등)이 옛 값을 덮어쓰지 않게 읽기 전용으로 둔다.
     */
    @Column(name = "favorite_count", nullable = false, insertable = false, updatable = false)
    private int favoriteCount;

    /** 후기 수 · 평균 평점 (후기 없으면 null). 찜 수와 같이 저장소 UPDATE 로만 바뀌고 평균은 DB 가 계산한다. */
    @Column(name = "review_count", nullable = false, insertable = false, updatable = false)
    private int reviewCount;

    @Column(name = "rating_average", insertable = false, updatable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage;

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

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<SpacePhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "space", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SpaceProof> proofs = new ArrayList<>();

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
     * 공간 정보를 바꾼다. 공개·일시 중단 중에 검수 항목(지역·주소·건물·호실·면적·용도)이 바뀌면 다시 검수받는다.
     *
     * @param availableDates null 이면 기존 캘린더를 유지한다
     */
    public void update(Region region, SpaceDetails details, Set<LocalDate> availableDates, LocalDate today) {
        validateEditable();
        boolean reviewedFieldsChanged = this.region != region || currentDetails().differsInReviewedFields(details);
        apply(region, details);
        if (availableDates != null) {
            changeAvailableDates(availableDates, today);
        }
        if (reviewedFieldsChanged) {
            requestReReviewIfPublic();
        }
    }

    /** 공개 중인 공간을 잠시 내린다. 진행 중인 신청은 그대로 처리할 수 있다. */
    public void pause() {
        transition(SpaceStatus.PUBLISHED, SpaceStatus.PAUSED, "공개 중인 공간만 일시 중단할 수 있습니다.");
    }

    public void resume() {
        transition(SpaceStatus.PAUSED, SpaceStatus.PUBLISHED, "일시 중단한 공간만 다시 공개할 수 있습니다.");
    }

    /** 수정 요청·공개 보류를 받은 공간을 고친 뒤 다시 검수를 요청한다. */
    public void resubmit() {
        if (status != SpaceStatus.REVISION_REQUESTED && status != SpaceStatus.ON_HOLD) {
            throw new BusinessException(ErrorCode.INVALID_SPACE_STATUS,
                    "수정 요청 또는 공개 보류 상태에서만 다시 검수를 요청할 수 있습니다.");
        }
        status = SpaceStatus.PENDING_REVIEW;
    }

    /**
     * 관리자 검수 결과를 반영하고 이력을 만든다. (이력 저장은 서비스가 맡는다)
     * <ul>
     *   <li>검수 대기 상태에서만 할 수 있다.</li>
     *   <li>공개 승인: 증빙 1개 이상 + 5개 검수 항목 모두 확인</li>
     *   <li>수정 요청 · 공개 보류: 사유 필수 (제공자에게 그대로 보인다)</li>
     * </ul>
     */
    public SpaceReview review(Member admin, ReviewDecision decision, String reason, ReviewChecklist checklist,
                              LocalDateTime now) {
        if (status != SpaceStatus.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.INVALID_SPACE_STATUS, "검수 대기 중인 공간만 검수할 수 있습니다.");
        }
        String normalizedReason = blankToNull(reason);
        if (decision == ReviewDecision.APPROVE) {
            validateApprovable(checklist);
        } else if (normalizedReason == null) {
            throw new BusinessException(ErrorCode.REVIEW_REASON_REQUIRED);
        }
        this.status = decision.getResultStatus();
        this.reviewComment = normalizedReason;
        this.reviewedAt = now;
        return new SpaceReview(this, admin, decision, normalizedReason, checklist, now);
    }

    private void validateApprovable(ReviewChecklist checklist) {
        if (proofs.isEmpty()) {
            throw new BusinessException(ErrorCode.SPACE_PROOF_REQUIRED);
        }
        if (!checklist.allPassed()) {
            throw new BusinessException(ErrorCode.REVIEW_CHECKLIST_INCOMPLETE);
        }
    }

    /** 신고 처리로 관리자가 숨긴다. 이용자 화면에서 사라지고 제공자는 수정할 수 없다. */
    public void hide(String reason, LocalDateTime now) {
        this.status = SpaceStatus.HIDDEN;
        this.reviewComment = reason;
        this.reviewedAt = now;
    }

    private void transition(SpaceStatus from, SpaceStatus to, String message) {
        if (status != from) {
            throw new BusinessException(ErrorCode.INVALID_SPACE_STATUS, message);
        }
        status = to;
    }

    /** @throws BusinessException 본인 공간이 아니면 NOT_SPACE_OWNER */
    public void validateOwner(Long memberId) {
        if (!isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_SPACE_OWNER);
        }
    }

    /** @throws BusinessException 관리자가 숨긴 공간이면 SPACE_NOT_EDITABLE */
    public void validateEditable() {
        if (status == SpaceStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.SPACE_NOT_EDITABLE);
        }
    }

    /** 사진을 맨 뒤에 추가한다. */
    public SpacePhoto addPhoto(String storagePath, LocalDateTime now) {
        validateEditable();
        if (photos.size() >= MAX_PHOTOS) {
            throw new BusinessException(ErrorCode.SPACE_PHOTO_LIMIT_EXCEEDED);
        }
        int nextOrder = photos.stream().mapToInt(SpacePhoto::getSortOrder).max().orElse(-1) + 1;
        SpacePhoto photo = new SpacePhoto(this, storagePath, nextOrder, now);
        photos.add(photo);
        return photo;
    }

    public void validateCanAddPhotos(int count) {
        validateEditable();
        if (photos.size() + count > MAX_PHOTOS) {
            throw new BusinessException(ErrorCode.SPACE_PHOTO_LIMIT_EXCEEDED,
                    "사진은 최대 %d장까지 올릴 수 있습니다. (현재 %d장)".formatted(MAX_PHOTOS, photos.size()));
        }
    }

    /** @return 지운 사진 (저장소 파일 삭제용) */
    public SpacePhoto removePhoto(Long photoId) {
        validateEditable();
        SpacePhoto photo = photos.stream().filter(p -> p.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_PHOTO_NOT_FOUND));
        photos.remove(photo);
        return photo;
    }

    /**
     * 사진 순서를 바꾼다. 첫 번째가 대표 사진이 된다.
     *
     * @param photoIds 이 공간의 모든 사진 ID 를 원하는 순서대로 한 번씩
     */
    public void reorderPhotos(List<Long> photoIds) {
        validateEditable();
        Set<Long> currentIds = new HashSet<>();
        photos.forEach(photo -> currentIds.add(photo.getId()));
        if (photoIds.size() != photos.size() || !currentIds.equals(new HashSet<>(photoIds))) {
            throw new BusinessException(ErrorCode.INVALID_PHOTO_ORDER);
        }
        for (SpacePhoto photo : photos) {
            photo.moveTo(photoIds.indexOf(photo.getId()));
        }
        photos.sort(Comparator.comparingInt(SpacePhoto::getSortOrder));
    }

    /** 증빙이 바뀌면 공개 중인 공간도 다시 검수받는다. */
    public SpaceProof addProof(ProofType proofType, SpaceProof.StoredProofFile file, LocalDateTime now) {
        validateEditable();
        if (proofs.size() >= MAX_PROOFS) {
            throw new BusinessException(ErrorCode.SPACE_PROOF_LIMIT_EXCEEDED);
        }
        SpaceProof proof = new SpaceProof(this, proofType, file, now);
        proofs.add(proof);
        requestReReviewIfPublic();
        return proof;
    }

    /** @return 지운 증빙 (저장소 파일 삭제용) */
    public SpaceProof removeProof(Long proofId) {
        validateEditable();
        SpaceProof proof = proofs.stream().filter(p -> p.getId().equals(proofId)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_PROOF_NOT_FOUND));
        proofs.remove(proof);
        requestReReviewIfPublic();
        return proof;
    }

    /** 공개·일시 중단 중에 검수 항목이 바뀌면 검수 대기로 되돌린다. */
    private void requestReReviewIfPublic() {
        if (status == SpaceStatus.PUBLISHED || status == SpaceStatus.PAUSED) {
            status = SpaceStatus.PENDING_REVIEW;
        }
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
