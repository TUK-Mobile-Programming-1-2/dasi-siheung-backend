package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.RejectApplicationRequest;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공간 제공자가 받은 신청 조회 · 승인 · 거절. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderApplicationService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ApplicationRepository applicationRepository;
    private final SpaceRepository spaceRepository;
    private final ApplicationResponseMapper responseMapper;
    private final Clock clock;

    /**
     * @param status  null 이면 전체
     * @param spaceId null 이면 내 모든 공간
     */
    public PageResponse<ApplicationSummaryResponse> getReceivedApplications(Long providerId, ApplicationStatus status,
                                                                            Long spaceId, PageQuery pageQuery) {
        return responseMapper.summaries(applicationRepository.findReceived(providerId, status, spaceId,
                pageQuery.toPageable(LATEST_FIRST)));
    }

    /**
     * 승인해 예약을 확정한다. 같은 공간의 승인은 공간 행 잠금으로 한 번에 하나씩 처리한다.
     * <ol>
     *   <li>공간 행을 잠근다 (SELECT ... FOR UPDATE)</li>
     *   <li>잠근 뒤 신청을 새로 읽고, 겹치는 확정 예약이 있는지 조회한다 (상태를 바꾸기 전에)</li>
     *   <li>상태 · 협의 · 기간 · 겹침을 검사해 확정한다 (겹치면 PERIOD_ALREADY_BOOKED)</li>
     *   <li>확정하고, 겹치는 다른 검토 중 신청은 사유와 함께 마감(CLOSED)한다</li>
     * </ol>
     * 잠금을 우회한 경우에도 DB 배타 제약(V9)이 겹치는 확정을 막는다.
     */
    @Transactional
    public ApplicationResponse approve(Long providerId, Long applicationId) {
        Long spaceId = applicationRepository.findSpaceIdById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLICATION_NOT_FOUND));
        Space space = spaceRepository.findWithLockById(spaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
        Application application = applicationRepository.getApplication(applicationId);
        LocalDateTime now = LocalDateTime.now(clock);

        boolean periodBooked = applicationRepository.existsOverlapping(spaceId, ApplicationStatus.APPROVED,
                application.getStartDate(), application.getEndDate());
        application.approve(providerId, periodBooked, LocalDate.now(clock), now);
        applicationRepository.flush();
        closeOverlappingPending(space, application, now);
        return responseMapper.detail(application);
    }

    private void closeOverlappingPending(Space space, Application approved, LocalDateTime now) {
        List<Application> overlapping = applicationRepository.findOverlapping(space.getId(), ApplicationStatus.PENDING,
                approved.getStartDate(), approved.getEndDate());
        overlapping.forEach(application -> application.closeBecauseBooked(now));
    }

    @Transactional
    public ApplicationResponse reject(Long providerId, Long applicationId, RejectApplicationRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        application.reject(providerId, request.reason(), LocalDateTime.now(clock));
        applicationRepository.flush();
        return responseMapper.detail(application);
    }
}
