package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.application.PriceOfferRequest;
import com.dasisiheung.dto.application.RejectOfferRequest;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.PriceNegotiationStatus;
import com.dasisiheung.entity.application.PriceOffer;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.notification.NotificationType;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PriceOfferRepository;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceNegotiationServiceTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;
    private static final long APPLICATION_ID = 31L;
    private static final long APPLICANT_ID = 1L;
    private static final long PROVIDER_ID = 2L;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PriceOfferRepository priceOfferRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationResponseMapper responseMapper;

    @Mock
    private NotificationService notificationService;

    private PriceNegotiationService service;
    private Application application;
    private Member provider;

    @BeforeEach
    void setUp() {
        service = new PriceNegotiationService(applicationRepository, priceOfferRepository, memberRepository,
                responseMapper, notificationService, FixedClockConfig.fixedClock());
        LocalDate start = TODAY.plusDays(1);
        LocalDate end = TODAY.plusDays(2);
        provider = SpaceFixtures.provider(PROVIDER_ID);
        application = ApplicationFixtures.application(APPLICATION_ID, ApplicationFixtures.publishedSpace(
                        SpaceFixtures.space(10L, provider, Set.of(start, end), TODAY)),
                ApplicationFixtures.applicant(APPLICANT_ID), start, end, TODAY);
        given(applicationRepository.getApplication(APPLICATION_ID)).willReturn(application);
    }

    @Test
    @DisplayName("제공자 역제안을 저장하면 신청자에게 PRICE_OFFER_RECEIVED 알림을 보낸다")
    void counterOfferNotifiesApplicant() {
        given(memberRepository.getReferenceById(PROVIDER_ID)).willReturn(provider);

        service.offer(PROVIDER_ID, APPLICATION_ID, new PriceOfferRequest(95_000, "9만 5천원까지 가능합니다."));

        then(priceOfferRepository).should().save(any(PriceOffer.class));
        NotificationDraft draft = sentDraft();
        assertThat(draft.recipientId()).isEqualTo(APPLICANT_ID);
        assertThat(draft.type()).isEqualTo(NotificationType.PRICE_OFFER_RECEIVED);
        assertThat(draft.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    @DisplayName("제안이 거부되면(내 제안의 답을 기다리는 중) 알림을 보내지 않는다")
    void noNotificationWhenOfferFails() {
        given(memberRepository.getReferenceById(PROVIDER_ID)).willReturn(provider);
        application.offer(provider, 95_000, null, FixedClockConfig.FIXED_LOCAL_NOW);

        assertThatThrownBy(() -> service.offer(PROVIDER_ID, APPLICATION_ID, new PriceOfferRequest(90_000, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.OFFER_NOT_ALLOWED);
        then(notificationService).should(never()).send(any());
    }

    @Test
    @DisplayName("신청자가 역제안을 수락하면 제안한 공간 제공자에게 PRICE_OFFER_ACCEPTED")
    void acceptNotifiesProposer() {
        PriceOffer counter = application.offer(provider, 95_000, null, FixedClockConfig.FIXED_LOCAL_NOW);
        given(priceOfferRepository.findFirstByApplication_IdOrderByIdDesc(APPLICATION_ID)).willReturn(Optional.of(counter));

        service.accept(APPLICANT_ID, APPLICATION_ID);

        assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.AGREED);
        NotificationDraft draft = sentDraft();
        assertThat(draft.recipientId()).isEqualTo(PROVIDER_ID);
        assertThat(draft.type()).isEqualTo(NotificationType.PRICE_OFFER_ACCEPTED);
    }

    @Test
    @DisplayName("신청자가 역제안을 거절하면 제안한 공간 제공자에게 PRICE_OFFER_REJECTED, 신청은 검토 중으로 남는다")
    void rejectNotifiesProposer() {
        PriceOffer counter = application.offer(provider, 95_000, null, FixedClockConfig.FIXED_LOCAL_NOW);
        given(priceOfferRepository.findFirstByApplication_IdOrderByIdDesc(APPLICATION_ID)).willReturn(Optional.of(counter));

        service.reject(APPLICANT_ID, APPLICATION_ID, new RejectOfferRequest("주말이라 어렵습니다."));

        assertThat(application.getNegotiationStatus()).isEqualTo(PriceNegotiationStatus.OFFER_REJECTED);
        assertThat(counter.getRejectionMessage()).isEqualTo("주말이라 어렵습니다.");
        NotificationDraft draft = sentDraft();
        assertThat(draft.recipientId()).isEqualTo(PROVIDER_ID);
        assertThat(draft.type()).isEqualTo(NotificationType.PRICE_OFFER_REJECTED);
    }

    @Test
    @DisplayName("답할 제안이 없으면 NO_PENDING_OFFER, 알림은 보내지 않는다")
    void rejectWithoutPendingOffer() {
        given(priceOfferRepository.findFirstByApplication_IdOrderByIdDesc(APPLICATION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(APPLICANT_ID, APPLICATION_ID, RejectOfferRequest.EMPTY))
                .extracting("errorCode").isEqualTo(ErrorCode.NO_PENDING_OFFER);
        then(notificationService).should(never()).send(any());
    }

    private NotificationDraft sentDraft() {
        ArgumentCaptor<NotificationDraft> draft = ArgumentCaptor.forClass(NotificationDraft.class);
        then(notificationService).should().send(draft.capture());
        return draft.getValue();
    }
}
