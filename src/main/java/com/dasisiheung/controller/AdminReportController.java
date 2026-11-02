package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.report.AdminReportResponse;
import com.dasisiheung.dto.report.MemberSuspensionResponse;
import com.dasisiheung.dto.report.ResolveReportRequest;
import com.dasisiheung.dto.report.SuspendRequest;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.AdminReportService;
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

/** 관리자(ADMIN) 신고 처리 · 이용 정지. */
@Tag(name = "Admin Report", description = "관리자 — 신고 처리 · 이용 정지")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "신고 목록", description = "status=PENDING 이면 오래된 순, 그 밖은 최근 순.")
    @GetMapping("/reports")
    public ApiResult<PageResponse<AdminReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(adminReportService.getReports(status, targetType, pageQuery));
    }

    @Operation(summary = "신고 상세")
    @GetMapping("/reports/{reportId}")
    public ApiResult<AdminReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResult.ok(adminReportService.getReport(reportId));
    }

    @Operation(summary = "신고 처리",
            description = "DISMISS 기각 | HIDE 대상 숨김 | SUSPEND 작성자 · 제공자 이용 정지 | HIDE_AND_SUSPEND. "
                    + "같은 대상의 처리 대기 신고도 함께 닫힙니다.")
    @PostMapping("/reports/{reportId}/resolve")
    public ApiResult<AdminReportResponse> resolve(@LoginMember AuthMember loginMember, @PathVariable Long reportId,
                                                  @Valid @RequestBody ResolveReportRequest request) {
        return ApiResult.ok(adminReportService.resolve(loginMember.id(), reportId, request));
    }

    @Operation(summary = "회원 이용 정지", description = "로그인 · 토큰 재발급이 막힙니다. 이미 발급된 액세스 토큰은 최대 30분 유효합니다.")
    @PostMapping("/members/{memberId}/suspend")
    public ApiResult<MemberSuspensionResponse> suspend(@PathVariable Long memberId,
                                                       @Valid @RequestBody SuspendRequest request) {
        return ApiResult.ok(adminReportService.suspend(memberId, request.days()));
    }

    @Operation(summary = "이용 정지 해제")
    @PostMapping("/members/{memberId}/unsuspend")
    public ApiResult<MemberSuspensionResponse> unsuspend(@PathVariable Long memberId) {
        return ApiResult.ok(adminReportService.unsuspend(memberId));
    }
}
