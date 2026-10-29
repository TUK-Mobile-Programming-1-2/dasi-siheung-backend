package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.RejectApplicationRequest;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.ProviderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공간 제공자(PROVIDER)가 받은 신청. 상세 · 가격 협의는 /api/v1/applications/{id} 를 함께 쓴다. */
@Tag(name = "Provider Application", description = "공간 제공자 — 받은 신청 · 승인 · 거절")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/provider/applications")
@RequiredArgsConstructor
public class ProviderApplicationController {

    private final ProviderApplicationService providerApplicationService;

    @Operation(summary = "받은 신청 목록", description = "최근 순. status · spaceId 로 거를 수 있습니다.")
    @GetMapping
    public ApiResult<PageResponse<ApplicationSummaryResponse>> getReceivedApplications(
            @LoginMember AuthMember loginMember, @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long spaceId, @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(providerApplicationService.getReceivedApplications(loginMember.id(), status, spaceId,
                pageQuery));
    }

    @Operation(summary = "신청 승인 (예약 확정)",
            description = "가격 협의가 끝난 검토 중 신청만 승인할 수 있습니다. 겹치는 기간의 다른 검토 중 신청은 자동으로 마감됩니다.")
    @PostMapping("/{applicationId}/approve")
    public ApiResult<ApplicationResponse> approve(@LoginMember AuthMember loginMember, @PathVariable Long applicationId) {
        return ApiResult.ok(providerApplicationService.approve(loginMember.id(), applicationId));
    }

    @Operation(summary = "신청 거절", description = "검토 중인 신청만 거절할 수 있고, 사유는 신청자에게 보입니다.")
    @PostMapping("/{applicationId}/reject")
    public ApiResult<ApplicationResponse> reject(@LoginMember AuthMember loginMember, @PathVariable Long applicationId,
                                                 @Valid @RequestBody RejectApplicationRequest request) {
        return ApiResult.ok(providerApplicationService.reject(loginMember.id(), applicationId, request));
    }
}
