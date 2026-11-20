package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.PriceOfferRequest;
import com.dasisiheung.dto.application.RejectOfferRequest;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.PriceOffer;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PriceOfferRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청자와 공간 제공자의 가격 제안 · 역제안 · 수락 · 거절. 규칙은 {@link Application#offer} 참고.
 * 두 사람이 동시에 답하면 신청의 버전 충돌로 한쪽은 409 DATA_CONFLICT 를 받는다.
 * <p>
 * 제안은 상대에게, 수락 · 거절은 제안한 사람에게 알림함 + 푸시로 알린다. ({@link PriceOfferNotices})
 * 신청서와 함께 보낸 첫 제안은 새 신청 알림(APPLICATION_SUBMITTED)에 포함되므로 여기서 따로 알리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PriceNegotiationService {

    private final ApplicationRepository applicationRepository;
    private final PriceOfferRepository priceOfferRepository;
    private final MemberRepository memberRepository;
    private final ApplicationResponseMapper responseMapper;
    private final NotificationService notificationService;
    private final Clock clock;

    public ApplicationResponse offer(Long memberId, Long applicationId, PriceOfferRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        Member proposer = memberRepository.getReferenceById(memberId);

        PriceOffer offer = application.offer(proposer, request.amount(), request.message(), LocalDateTime.now(clock));
        priceOfferRepository.save(offer);
        applicationRepository.flush();
        notificationService.send(PriceOfferNotices.received(application, offer));
        return responseMapper.detail(application);
    }

    public ApplicationResponse accept(Long memberId, Long applicationId) {
        Application application = applicationRepository.getApplication(applicationId);
        PriceOffer latestOffer = latestOffer(applicationId);

        application.acceptOffer(memberId, latestOffer, LocalDateTime.now(clock));
        applicationRepository.flush();
        notificationService.send(PriceOfferNotices.accepted(application, latestOffer));
        return responseMapper.detail(application);
    }

    /** 받은 제안을 거절한다. 신청은 검토 중으로 남고 양쪽 모두 다시 제안할 수 있다. */
    public ApplicationResponse reject(Long memberId, Long applicationId, RejectOfferRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        PriceOffer latestOffer = latestOffer(applicationId);

        application.rejectOffer(memberId, latestOffer, request.message(), LocalDateTime.now(clock));
        applicationRepository.flush();
        notificationService.send(PriceOfferNotices.rejected(application, latestOffer));
        return responseMapper.detail(application);
    }

    private PriceOffer latestOffer(Long applicationId) {
        return priceOfferRepository.findFirstByApplication_IdOrderByIdDesc(applicationId).orElse(null);
    }
}
