package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.application.ApplicationRequest;
import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.CancelApplicationRequest;
import com.dasisiheung.dto.application.PriceOfferRequest;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.ApplicationService;
import com.dasisiheung.service.PriceNegotiationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 공간 이용 신청. 신청 · 목록 · 취소는 이용자(USER), 상세 · 가격 협의는 신청자와 공간 제공자 모두. */
@Tag(name = "Application", description = "공간 이용 신청")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final PriceNegotiationService priceNegotiationService;

    @Operation(summary = "신청서 제출", description = "공개 공간 · 허용 활동 · 사용 가능 날짜 안의 기간(최대 30일)만 신청할 수 있습니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public ApiResult<ApplicationResponse> submit(@LoginMember AuthMember loginMember,
                                                 @Valid @RequestBody ApplicationRequest request) {
        return ApiResult.ok(applicationService.submit(loginMember.id(), request));
    }

    @Operation(summary = "내가 보낸 신청", description = "최근 순. status 로 거를 수 있습니다.")
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResult<PageResponse<ApplicationSummaryResponse>> getMyApplications(
            @LoginMember AuthMember loginMember, @RequestParam(required = false) ApplicationStatus status,
            @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(applicationService.getMyApplications(loginMember.id(), status, pageQuery));
    }

    @Operation(summary = "신청 상세", description = "신청자와 해당 공간 제공자만 볼 수 있습니다.")
    @GetMapping("/{applicationId}")
    public ApiResult<ApplicationResponse> getApplication(@LoginMember AuthMember loginMember,
                                                         @PathVariable Long applicationId) {
        return ApiResult.ok(applicationService.getApplication(loginMember.id(), applicationId));
    }

    @Operation(summary = "신청 취소", description = "검토 중이면 언제든, 확정된 예약은 이용 시작일 전까지 취소할 수 있습니다.")
    @PostMapping("/{applicationId}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<ApplicationResponse> cancel(@LoginMember AuthMember loginMember, @PathVariable Long applicationId,
                                                 @Valid @RequestBody CancelApplicationRequest request) {
        return ApiResult.ok(applicationService.cancel(loginMember.id(), applicationId, request));
    }

    @Operation(summary = "가격 제안 · 역제안",
            description = "신청자 · 공간 제공자 모두 보낼 수 있습니다. 금액은 기간 합계(1원~정가 합계)이며, "
                    + "보낸 제안에 상대가 답하기 전에는 다시 보낼 수 없습니다.")
    @PostMapping("/{applicationId}/offers")
    public ApiResult<ApplicationResponse> offer(@LoginMember AuthMember loginMember, @PathVariable Long applicationId,
                                                @Valid @RequestBody PriceOfferRequest request) {
        return ApiResult.ok(priceNegotiationService.offer(loginMember.id(), applicationId, request));
    }

    @Operation(summary = "받은 가격 제안 수락", description = "상대가 보낸 최근 제안 금액으로 합의합니다.")
    @PostMapping("/{applicationId}/offers/accept")
    public ApiResult<ApplicationResponse> acceptOffer(@LoginMember AuthMember loginMember,
                                                      @PathVariable Long applicationId) {
        return ApiResult.ok(priceNegotiationService.accept(loginMember.id(), applicationId));
    }
}
