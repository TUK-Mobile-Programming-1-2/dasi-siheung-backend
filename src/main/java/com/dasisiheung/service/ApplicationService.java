package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationRequest;
import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.CancelApplicationRequest;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationPeriod;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PriceOfferRepository;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이용자의 공간 이용 신청 · 조회 · 취소. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ApplicationRepository applicationRepository;
    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final PriceOfferRepository priceOfferRepository;
    private final ApplicationResponseMapper responseMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 신청서를 낸다. 공개 공간 · 허용 활동 · 사용 가능 날짜 안의 기간이어야 하고,
     * 같은 공간 · 겹치는 기간에 검토 중인 내 신청이 있으면 다시 낼 수 없다.
     * 제안 금액을 함께 보내면 첫 가격 제안으로 남는다.
     *
     * @throws BusinessException 확정 예약과 겹치면 PERIOD_ALREADY_BOOKED, 중복이면 DUPLICATE_APPLICATION,
     *                           협의를 받지 않는 공간에 제안 금액을 보내면 PRICE_NEGOTIATION_NOT_ALLOWED,
     *                           그 밖은 {@link Application#submit} 참고
     */
    @Transactional
    public ApplicationResponse submit(Long applicantId, ApplicationRequest request) {
        Member applicant = getActiveMember(applicantId);
        Space space = spaceRepository.getSpace(request.spaceId());
        ApplicationPeriod period = request.toPeriod();

        Application application = Application.submit(space, applicant, request.toActivityDetails(), period, today());
        if (request.hasOffer()) {
            space.validatePriceNegotiable();
        }
        validateNotBooked(space, period);
        validateNoDuplicate(space, applicantId, period);
        applicationRepository.save(application);
        if (request.hasOffer()) {
            priceOfferRepository.save(application.offer(applicant, request.offerAmount(), request.offerMessage(),
                    LocalDateTime.now(clock)));
        }
        eventPublisher.publishEvent(new ApplicationSubmittedEvent(application.getId(), space.getId(), space.getTitle(),
                space.getProvider().getId(), applicant.getName(), period.startDate(), period.endDate(),
                request.hasOffer()));
        return responseMapper.detail(application);
    }

    /** 내가 보낸 신청 (최근 순). status 가 null 이면 전체. */
    public PageResponse<ApplicationSummaryResponse> getMyApplications(Long applicantId, ApplicationStatus status,
                                                                      PageQuery pageQuery) {
        Pageable pageable = pageQuery.toPageable(LATEST_FIRST);
        Page<Application> applications = status == null
                ? applicationRepository.findByApplicant_Id(applicantId, pageable)
                : applicationRepository.findByApplicant_IdAndStatus(applicantId, status, pageable);
        return responseMapper.summaries(applications);
    }

    /** 신청 상세. 신청자와 해당 공간 제공자만 볼 수 있다. */
    public ApplicationResponse getApplication(Long memberId, Long applicationId) {
        Application application = applicationRepository.getApplication(applicationId);
        application.validateParticipant(memberId);
        return responseMapper.detail(application);
    }

    @Transactional
    public ApplicationResponse cancel(Long applicantId, Long applicationId, CancelApplicationRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        application.cancel(applicantId, request.reason(), today(), LocalDateTime.now(clock));
        applicationRepository.flush();
        return responseMapper.detail(application);
    }

    private void validateNotBooked(Space space, ApplicationPeriod period) {
        boolean booked = applicationRepository.existsOverlapping(space.getId(), ApplicationStatus.APPROVED,
                period.startDate(), period.endDate());
        if (booked) {
            throw new BusinessException(ErrorCode.PERIOD_ALREADY_BOOKED);
        }
    }

    private void validateNoDuplicate(Space space, Long applicantId, ApplicationPeriod period) {
        boolean duplicated = applicationRepository.existsOverlappingByApplicant(space.getId(), applicantId,
                ApplicationStatus.PENDING, period.startDate(), period.endDate());
        if (duplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }
    }

    private Member getActiveMember(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
