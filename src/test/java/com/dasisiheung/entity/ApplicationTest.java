package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationPeriod;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.entity.application.PriceOffer;
import com.dasisiheung.entity.application.PriceNegotiationStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ApplicationTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;
    private static final LocalDateTime NOW = FixedClockConfig.FIXED_LOCAL_NOW;
    private static final long APPLICANT_ID = 1L;

    private Space space;
    private Member applicant;

    @BeforeEach
    void setUp() {
        Set<LocalDate> dates = Set.of(TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3));
        space = ApplicationFixtures.publishedSpace(SpaceFixtures.space(10L, SpaceFixtures.provider(2L), dates, TODAY));
        applicant = ApplicationFixtures.applicant(APPLICANT_ID);
    }

    @Nested
    @DisplayName("신청")
    class Submit {

        @Test
        @DisplayName("정가는 신청 시점 1일 이용료 × 일수로 고정하고 검토 중 · 협의 없음으로 시작한다")
        void listPrice() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(2));

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(application.getListDailyPrice()).isEqualTo(50_000);
            assertThat(application.getListTotalPrice()).isEqualTo(100_000);
            assertThat(application.getTotalPrice()).isEqualTo(100_000);
            assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.NONE);
        }

        @Test
        @DisplayName("공개 중이 아닌 공간은 SPACE_NOT_FOUND")
        void notPublished() {
            space.pause();

            assertThatThrownBy(() -> submit(TODAY.plusDays(1), TODAY.plusDays(1)))
                    .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }

        @Test
        @DisplayName("공간이 허용하지 않는 활동은 ACTIVITY_NOT_ALLOWED")
        void activityNotAllowed() {
            assertThatThrownBy(() -> Application.submit(space, applicant,
                    new Application.ActivityDetails(ActivityType.CLASS, "원데이 클래스", "계획", 5),
                    new ApplicationPeriod(TODAY.plusDays(1), TODAY.plusDays(1)), TODAY))
                    .extracting("errorCode").isEqualTo(ErrorCode.ACTIVITY_NOT_ALLOWED);
        }

        @Test
        @DisplayName("지난 날짜에 시작하거나 30일을 넘으면 INVALID_APPLICATION_PERIOD")
        void invalidPeriod() {
            assertThatThrownBy(() -> submit(TODAY.minusDays(1), TODAY.plusDays(1)))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPLICATION_PERIOD);
            assertThatThrownBy(() -> submit(TODAY.plusDays(1), TODAY.plusDays(31)))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPLICATION_PERIOD);
            assertThatThrownBy(() -> new ApplicationPeriod(TODAY.plusDays(2), TODAY.plusDays(1)))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPLICATION_PERIOD);
        }

        @Test
        @DisplayName("기간 중 하루라도 사용 가능 날짜가 아니면 PERIOD_NOT_AVAILABLE")
        void periodNotAvailable() {
            assertThatThrownBy(() -> submit(TODAY.plusDays(3), TODAY.plusDays(4)))
                    .extracting("errorCode").isEqualTo(ErrorCode.PERIOD_NOT_AVAILABLE);
        }
    }

    @Nested
    @DisplayName("기간")
    class Period {

        @Test
        @DisplayName("양 끝 날짜를 포함해 겹침을 판단한다")
        void overlaps() {
            ApplicationPeriod period = new ApplicationPeriod(TODAY.plusDays(1), TODAY.plusDays(3));

            assertThat(period.days()).isEqualTo(3);
            assertThat(period.overlaps(new ApplicationPeriod(TODAY.plusDays(3), TODAY.plusDays(5)))).isTrue();
            assertThat(period.overlaps(new ApplicationPeriod(TODAY, TODAY.plusDays(1)))).isTrue();
            assertThat(period.overlaps(new ApplicationPeriod(TODAY.plusDays(4), TODAY.plusDays(5)))).isFalse();
        }
    }

    @Nested
    @DisplayName("취소")
    class Cancel {

        @Test
        @DisplayName("검토 중인 신청은 사유와 함께 취소된다")
        void cancelPending() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(1));

            application.cancel(APPLICANT_ID, " 일정 변경 ", TODAY, NOW);

            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
            assertThat(application.getStatusReason()).isEqualTo("일정 변경");
            assertThat(application.getDecidedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("신청자가 아니면 NOT_APPLICATION_PARTICIPANT")
        void notApplicant() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(1));

            assertThatThrownBy(() -> application.cancel(99L, null, TODAY, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_APPLICATION_PARTICIPANT);
        }

        @Test
        @DisplayName("확정된 예약은 이용 시작일부터 취소할 수 없다")
        void approvedStarted() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(2));
            ReflectionTestUtils.setField(application, "status", ApplicationStatus.APPROVED);

            assertThatThrownBy(() -> application.cancel(APPLICANT_ID, null, TODAY.plusDays(1), NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.APPLICATION_NOT_CANCELLABLE);
            application.cancel(APPLICANT_ID, null, TODAY, NOW);
            assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("이미 끝난 신청은 INVALID_APPLICATION_STATUS")
        void finished() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(1));
            application.cancel(APPLICANT_ID, null, TODAY, NOW);

            assertThatThrownBy(() -> application.cancel(APPLICANT_ID, null, TODAY, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPLICATION_STATUS);
        }
    }

    @Nested
    @DisplayName("가격 협의")
    class Negotiation {

        private final Member provider = SpaceFixtures.provider(2L);

        @Test
        @DisplayName("신청자 제안 → 제공자 역제안 → 신청자 수락이면 역제안 금액으로 합의한다")
        void counterOfferAndAccept() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(2));

            PriceOffer first = application.offer(applicant, 80_000, "8만원 가능할까요?", NOW);
            assertThat(first.getProposerSide()).isEqualTo(OfferSide.APPLICANT);
            assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.WAITING_PROVIDER);

            PriceOffer counter = application.offer(provider, 95_000, null, NOW);
            assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.WAITING_APPLICANT);
            assertThat(application.getPendingOfferAmount()).isEqualTo(95_000);

            application.acceptOffer(APPLICANT_ID, counter, NOW);
            assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.AGREED);
            assertThat(application.getTotalPrice()).isEqualTo(95_000);
            assertThat(application.getPendingOfferAmount()).isNull();
            assertThat(counter.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("내 제안에 답을 기다리는 중에는 다시 제안 · 수락할 수 없다")
        void waitingForCounterpart() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(2));
            PriceOffer first = application.offer(applicant, 80_000, null, NOW);

            assertThatThrownBy(() -> application.offer(applicant, 70_000, null, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.OFFER_NOT_ALLOWED);
            assertThatThrownBy(() -> application.acceptOffer(APPLICANT_ID, first, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.NO_PENDING_OFFER);
        }

        @Test
        @DisplayName("제안 금액은 1원~정가 합계, 참여자만, 검토 중인 신청만")
        void offerRules() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(2));

            assertThatThrownBy(() -> application.offer(applicant, 100_001, null, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_OFFER_PRICE);
            assertThatThrownBy(() -> application.offer(applicant, 0, null, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_OFFER_PRICE);
            assertThatThrownBy(() -> application.offer(ApplicationFixtures.applicant(99L), 90_000, null, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.NOT_APPLICATION_PARTICIPANT);

            application.cancel(APPLICANT_ID, null, TODAY, NOW);
            assertThatThrownBy(() -> application.offer(applicant, 90_000, null, NOW))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_APPLICATION_STATUS);
        }

        @Test
        @DisplayName("답을 기다리는 제안이 있는 채로 취소하면 대기 제안은 사라지고 합의 금액은 그대로다")
        void cancelClearsPendingOffer() {
            Application application = submit(TODAY.plusDays(1), TODAY.plusDays(2));
            application.offer(applicant, 80_000, null, NOW);

            application.cancel(APPLICANT_ID, null, TODAY, NOW);

            assertThat(application.getPendingOfferAmount()).isNull();
            assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.NONE);
            assertThat(application.getTotalPrice()).isEqualTo(100_000);
        }
    }

    private Application submit(LocalDate startDate, LocalDate endDate) {
        return Application.submit(space, applicant,
                new Application.ActivityDetails(ActivityType.POPUP_STORE, " 캔들 팝업 ", "계획", 3),
                new ApplicationPeriod(startDate, endDate), TODAY);
    }
}
