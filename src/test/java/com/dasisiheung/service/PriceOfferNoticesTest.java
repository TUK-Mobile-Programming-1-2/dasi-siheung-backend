package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.PriceOffer;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.notification.NotificationType;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriceOfferNoticesTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;
    private static final LocalDateTime NOW = FixedClockConfig.FIXED_LOCAL_NOW;
    private static final long APPLICANT_ID = 1L;
    private static final long PROVIDER_ID = 2L;
    private static final long APPLICATION_ID = 31L;

    private Application application;
    private Member applicant;
    private Member provider;

    @BeforeEach
    void setUp() {
        LocalDate start = TODAY.plusDays(1);
        LocalDate end = TODAY.plusDays(2);
        provider = SpaceFixtures.provider(PROVIDER_ID);
        applicant = ApplicationFixtures.applicant(APPLICANT_ID);
        application = ApplicationFixtures.application(APPLICATION_ID, ApplicationFixtures.publishedSpace(
                SpaceFixtures.space(10L, provider, Set.of(start, end), TODAY)), applicant, start, end, TODAY);
    }

    @Test
    @DisplayName("제공자 역제안은 신청자에게 — 신청 ID · 누가 · 어느 공간 · 기간 · 금액")
    void counterOfferGoesToApplicant() {
        PriceOffer counter = application.offer(provider, 95_000, "9만 5천원까지 가능합니다.", NOW);

        NotificationDraft draft = PriceOfferNotices.received(application, counter);

        assertThat(draft.recipientId()).isEqualTo(APPLICANT_ID);
        assertThat(draft.type()).isEqualTo(NotificationType.PRICE_OFFER_RECEIVED);
        assertThat(draft.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(draft.title()).isEqualTo("공간 제공자의 역제안");
        assertThat(draft.body()).isEqualTo("제공자님이 '정왕역 앞 1층 상가' %s 이용 금액으로 95,000원을 제안했어요."
                .formatted(period()));
        assertThat(draft.body()).doesNotContain("9만 5천원까지");
    }

    @Test
    @DisplayName("신청자 제안은 공간 제공자에게")
    void applicantOfferGoesToProvider() {
        PriceOffer offer = application.offer(applicant, 80_000, null, NOW);

        NotificationDraft draft = PriceOfferNotices.received(application, offer);

        assertThat(draft.recipientId()).isEqualTo(PROVIDER_ID);
        assertThat(draft.title()).isEqualTo("신청자의 가격 제안");
        assertThat(draft.body()).startsWith("이이용님이 ");
    }

    @Test
    @DisplayName("수락 · 거절은 제안한 사람에게, 답한 사람 이름으로")
    void answersGoToProposer() {
        PriceOffer counter = application.offer(provider, 95_000, null, NOW);

        NotificationDraft accepted = PriceOfferNotices.accepted(application, counter);
        NotificationDraft rejected = PriceOfferNotices.rejected(application, counter);

        assertThat(accepted.recipientId()).isEqualTo(PROVIDER_ID);
        assertThat(accepted.type()).isEqualTo(NotificationType.PRICE_OFFER_ACCEPTED);
        assertThat(accepted.body()).isEqualTo("이이용님이 '정왕역 앞 1층 상가' %s 이용 금액 95,000원 제안을 수락했어요."
                .formatted(period()));
        assertThat(rejected.recipientId()).isEqualTo(PROVIDER_ID);
        assertThat(rejected.type()).isEqualTo(NotificationType.PRICE_OFFER_REJECTED);
        assertThat(rejected.title()).isEqualTo("가격 제안 거절");
        assertThat(rejected.body()).isEqualTo(
                "이이용님이 '정왕역 앞 1층 상가' %s 이용 금액 95,000원 제안을 거절했어요. 다시 제안할 수 있어요."
                        .formatted(period()));
    }

    private String period() {
        return NoticeTexts.period(application.getStartDate(), application.getEndDate());
    }
}
