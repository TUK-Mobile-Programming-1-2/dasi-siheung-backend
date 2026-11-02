package com.dasisiheung.entity.review;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 이용 후기. 이용이 끝난(종료일이 지난) 확정 예약의 신청자만 1번 쓸 수 있고, 공간 제공자가 답글을 단다.
 * 공간의 평점 합계 · 개수 갱신은 서비스가 저장소의 UPDATE 로 한다.
 */
@Getter
@Entity
@Table(name = "usage_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageReview extends BaseTimeEntity {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Member reviewer;

    /** DB 는 SMALLINT(1~5). 자바에서는 int 로 다룬다. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 500)
    private String reply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    private UsageReview(Application application, int rating, String content) {
        this.application = application;
        this.space = application.getSpace();
        this.reviewer = application.getApplicant();
        this.rating = rating;
        this.content = content.strip();
    }

    /**
     * @throws BusinessException 신청자가 아니면 NOT_APPLICATION_PARTICIPANT,
     *                           확정 예약이 아니거나 아직 이용이 끝나지 않았으면 REVIEW_NOT_ALLOWED
     */
    public static UsageReview write(Application application, Long reviewerId, int rating, String content,
                                    LocalDate today) {
        if (!application.isAppliedBy(reviewerId)) {
            throw new BusinessException(ErrorCode.NOT_APPLICATION_PARTICIPANT);
        }
        boolean used = application.getStatus() == ApplicationStatus.APPROVED && application.getEndDate().isBefore(today);
        if (!used) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        validateRating(rating);
        return new UsageReview(application, rating, content);
    }

    /** @return 평점 변화량 (공간 평점 합계에 더한다) */
    public int edit(Long reviewerId, int rating, String content) {
        validateOwner(reviewerId);
        validateRating(rating);
        int delta = rating - this.rating;
        this.rating = rating;
        this.content = content.strip();
        return delta;
    }

    /** 공간 제공자의 답글. 다시 보내면 고친다. */
    public void reply(Long providerId, String reply, LocalDateTime now) {
        if (!space.isOwnedBy(providerId)) {
            throw new BusinessException(ErrorCode.NOT_SPACE_OWNER);
        }
        this.reply = reply.strip();
        this.repliedAt = now;
    }

    public void validateOwner(Long reviewerId) {
        if (!reviewer.getId().equals(reviewerId)) {
            throw new BusinessException(ErrorCode.NOT_REVIEW_OWNER);
        }
    }

    private static void validateRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "평점은 1~5 사이여야 합니다.");
        }
    }
}
