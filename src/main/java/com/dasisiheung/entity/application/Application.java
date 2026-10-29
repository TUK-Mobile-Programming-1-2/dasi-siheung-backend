package com.dasisiheung.entity.application;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.Column;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공간 이용 신청. 이용자(USER)가 보내고 공간 제공자가 승인 · 거절한다.
 * <pre>
 * PENDING ─ 승인 → APPROVED ─ (시작 전) 취소 → CANCELLED
 *    ├─ 거절 → REJECTED
 *    ├─ 신청자 취소 → CANCELLED
 *    └─ 같은 기간 다른 신청 확정 → CLOSED
 * </pre>
 * 정가(1일 이용료 × 일수)는 신청 시점 값으로 고정한다.
 * <p>
 * 가격 협의: 한쪽이 제안하면 상대가 수락하거나 다시 제안(역제안)한다. 자기 제안에 대한 답을 기다리는 동안에는
 * 다시 제안할 수 없다. 제안 금액은 정가 합계를 넘을 수 없다.
 */
@Getter
@Entity
@Table(name = "space_application")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Member applicant;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "activity_title", nullable = false, length = 60)
    private String activityTitle;

    @Column(nullable = false, length = 2000)
    private String plan;

    @Column(nullable = false)
    private int headcount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "list_daily_price", nullable = false)
    private int listDailyPrice;

    @Column(name = "list_total_price", nullable = false)
    private int listTotalPrice;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "negotiation_status", nullable = false, length = 20)
    private PriceNegotiationStatus negotiationStatus;

    @Column(name = "pending_offer_amount")
    private Integer pendingOfferAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    private Application(Space space, Member applicant, ActivityDetails details, ApplicationPeriod period) {
        this.space = space;
        this.applicant = applicant;
        this.activityType = details.activityType();
        this.activityTitle = details.title().strip();
        this.plan = details.plan().strip();
        this.headcount = details.headcount();
        this.startDate = period.startDate();
        this.endDate = period.endDate();
        this.listDailyPrice = space.getDailyPrice();
        this.listTotalPrice = Math.multiplyExact(space.getDailyPrice(), period.days());
        this.totalPrice = listTotalPrice;
        this.negotiationStatus = PriceNegotiationStatus.NONE;
        this.status = ApplicationStatus.PENDING;
    }

    /**
     * 신청서를 만든다. 겹치는 신청 · 확정 예약 검사는 저장소 조회가 필요해 서비스가 맡는다.
     *
     * @throws BusinessException 공개 중이 아니면 SPACE_NOT_FOUND, 허용하지 않는 활동이면 ACTIVITY_NOT_ALLOWED,
     *                           기간이 잘못되면 INVALID_APPLICATION_PERIOD, 비어 있지 않은 날짜가 있으면 PERIOD_NOT_AVAILABLE
     */
    public static Application submit(Space space, Member applicant, ActivityDetails details,
                                     ApplicationPeriod period, LocalDate today) {
        if (space.getStatus() != SpaceStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.SPACE_NOT_FOUND);
        }
        if (!space.getActivityTypes().contains(details.activityType())) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_ALLOWED);
        }
        period.validateApplicable(today);
        if (!period.isCoveredBy(space.getAvailableDates())) {
            throw new BusinessException(ErrorCode.PERIOD_NOT_AVAILABLE);
        }
        return new Application(space, applicant, details, period);
    }

    public ApplicationPeriod period() {
        return new ApplicationPeriod(startDate, endDate);
    }

    public int days() {
        return period().days();
    }

    public boolean isAppliedBy(Long memberId) {
        return applicant.getId().equals(memberId);
    }

    public boolean isReceivedBy(Long memberId) {
        return space.isOwnedBy(memberId);
    }

    /** @throws BusinessException 신청자도 공간 제공자도 아니면 NOT_APPLICATION_PARTICIPANT */
    public void validateParticipant(Long memberId) {
        if (!isAppliedBy(memberId) && !isReceivedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_APPLICATION_PARTICIPANT);
        }
    }

    /** @throws BusinessException 신청자도 공간 제공자도 아니면 NOT_APPLICATION_PARTICIPANT */
    public OfferSide sideOf(Long memberId) {
        if (isAppliedBy(memberId)) {
            return OfferSide.APPLICANT;
        }
        if (isReceivedBy(memberId)) {
            return OfferSide.PROVIDER;
        }
        throw new BusinessException(ErrorCode.NOT_APPLICATION_PARTICIPANT);
    }

    /**
     * 가격을 제안한다. 처음 제안 · 받은 제안에 대한 역제안 · 합의 뒤 재협의 모두 이 메서드로 한다.
     *
     * @return 이력으로 저장할 제안
     * @throws BusinessException 검토 중이 아니면 INVALID_APPLICATION_STATUS, 금액이 1원~정가 합계 밖이면 INVALID_OFFER_PRICE,
     *                           내 제안에 대한 답을 기다리는 중이면 OFFER_NOT_ALLOWED
     */
    public PriceOffer offer(Member proposer, int amount, String message, LocalDateTime now) {
        OfferSide side = sideOf(proposer.getId());
        validatePending();
        if (amount < 1 || amount > listTotalPrice) {
            throw new BusinessException(ErrorCode.INVALID_OFFER_PRICE);
        }
        if (negotiationStatus == side.waitingForCounterpart()) {
            throw new BusinessException(ErrorCode.OFFER_NOT_ALLOWED);
        }
        this.pendingOfferAmount = amount;
        this.negotiationStatus = side.waitingForCounterpart();
        return new PriceOffer(this, proposer, side, amount, blankToNull(message), now);
    }

    /**
     * 받은 제안을 수락해 합의 금액으로 정한다.
     *
     * @param latestOffer 가장 최근 제안 (수락 시각을 남긴다)
     * @throws BusinessException 내가 답할 제안이 없으면 NO_PENDING_OFFER
     */
    public void acceptOffer(Long memberId, PriceOffer latestOffer, LocalDateTime now) {
        OfferSide side = sideOf(memberId);
        validatePending();
        if (negotiationStatus != side.waitingForMe() || latestOffer == null || latestOffer.getProposerSide() == side) {
            throw new BusinessException(ErrorCode.NO_PENDING_OFFER);
        }
        this.totalPrice = pendingOfferAmount;
        this.pendingOfferAmount = null;
        this.negotiationStatus = PriceNegotiationStatus.AGREED;
        latestOffer.accept(now);
    }

    private void validatePending() {
        if (status != ApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS, "검토 중인 신청만 가격을 협의할 수 있습니다.");
        }
    }

    /**
     * 신청자가 취소한다. 검토 중이면 언제든, 확정된 예약은 이용 시작 전까지만.
     */
    public void cancel(Long memberId, String reason, LocalDate today, LocalDateTime now) {
        if (!isAppliedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_APPLICATION_PARTICIPANT);
        }
        if (status == ApplicationStatus.APPROVED && !today.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_CANCELLABLE);
        }
        if (status != ApplicationStatus.PENDING && status != ApplicationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_APPLICATION_STATUS, "이미 끝난 신청은 취소할 수 없습니다.");
        }
        finish(ApplicationStatus.CANCELLED, blankToNull(reason), now);
    }

    private void finish(ApplicationStatus result, String reason, LocalDateTime now) {
        this.status = result;
        this.statusReason = reason;
        this.decidedAt = now;
        clearPendingOffer();
    }

    private void clearPendingOffer() {
        if (negotiationStatus.isWaiting()) {
            this.pendingOfferAmount = null;
            this.negotiationStatus = totalPrice == listTotalPrice ? PriceNegotiationStatus.NONE : PriceNegotiationStatus.AGREED;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * 신청자가 적는 활동 정보.
     *
     * @param title     활동 이름 (예: "수제 캔들 팝업")
     * @param plan      활동 계획 · 공간 사용 방법
     * @param headcount 예상 인원 (운영 인원 기준)
     */
    public record ActivityDetails(ActivityType activityType, String title, String plan, int headcount) {
    }
}
