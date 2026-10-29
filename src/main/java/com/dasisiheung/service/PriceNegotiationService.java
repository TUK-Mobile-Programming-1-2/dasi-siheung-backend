package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.PriceOfferRequest;
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
 * 신청자와 공간 제공자의 가격 제안 · 역제안 · 수락. 규칙은 {@link Application#offer} 참고.
 * 두 사람이 동시에 답하면 신청의 버전 충돌로 한쪽은 409 DATA_CONFLICT 를 받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PriceNegotiationService {

    private final ApplicationRepository applicationRepository;
    private final PriceOfferRepository priceOfferRepository;
    private final MemberRepository memberRepository;
    private final ApplicationResponseMapper responseMapper;
    private final Clock clock;

    public ApplicationResponse offer(Long memberId, Long applicationId, PriceOfferRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        Member proposer = memberRepository.getReferenceById(memberId);

        PriceOffer offer = application.offer(proposer, request.amount(), request.message(), LocalDateTime.now(clock));
        priceOfferRepository.save(offer);
        applicationRepository.flush();
        return responseMapper.detail(application);
    }

    public ApplicationResponse accept(Long memberId, Long applicationId) {
        Application application = applicationRepository.getApplication(applicationId);
        PriceOffer latestOffer = priceOfferRepository.findFirstByApplication_IdOrderByIdDesc(applicationId).orElse(null);

        application.acceptOffer(memberId, latestOffer, LocalDateTime.now(clock));
        applicationRepository.flush();
        return responseMapper.detail(application);
    }
}
