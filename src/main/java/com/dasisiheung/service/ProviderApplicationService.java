package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.RejectApplicationRequest;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.repository.ApplicationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공간 제공자가 받은 신청 조회 · 거절. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderApplicationService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final ApplicationRepository applicationRepository;
    private final ApplicationResponseMapper responseMapper;
    private final Clock clock;

    /**
     * @param status  null 이면 전체
     * @param spaceId null 이면 내 모든 공간
     */
    public PageResponse<ApplicationSummaryResponse> getReceivedApplications(Long providerId, ApplicationStatus status,
                                                                            Long spaceId, PageQuery pageQuery) {
        return PageResponse.from(applicationRepository.findReceived(providerId, status, spaceId,
                pageQuery.toPageable(LATEST_FIRST)), responseMapper::summary);
    }

    @Transactional
    public ApplicationResponse reject(Long providerId, Long applicationId, RejectApplicationRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        application.reject(providerId, request.reason(), LocalDateTime.now(clock));
        applicationRepository.flush();
        return responseMapper.detail(application);
    }
}
