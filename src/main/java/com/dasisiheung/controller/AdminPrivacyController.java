package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.privacy.MonthlyStatisticsResponse;
import com.dasisiheung.dto.privacy.PurgePreviewResponse;
import com.dasisiheung.dto.privacy.PurgeResultResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자(ADMIN) 개인정보 파기 · 이용 통계. */
@Tag(name = "Admin Privacy", description = "관리자 — 개인정보 파기 · 이용 통계")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPrivacyController {

    private final PrivacyService privacyService;

    @Operation(summary = "파기 미리보기", description = "지금 실행하면 비식별화 · 삭제될 양과 기준일.")
    @GetMapping("/privacy/purge-preview")
    public ApiResult<PurgePreviewResponse> preview() {
        return ApiResult.ok(privacyService.preview());
    }

    @Operation(summary = "파기 실행", description = "매일 04:30 자동 실행되는 작업을 지금 실행합니다. 여러 번 실행해도 결과가 같습니다.")
    @PostMapping("/privacy/purge")
    public ApiResult<PurgeResultResponse> purge(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(privacyService.purge(loginMember.id()));
    }

    @Operation(summary = "파기 실행 기록", description = "최근 순.")
    @GetMapping("/privacy/purge-logs")
    public ApiResult<PageResponse<PurgeResultResponse>> getLogs(@Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(privacyService.getLogs(pageQuery));
    }

    @Operation(summary = "월별 이용 통계", description = "어제까지 이용이 끝난 확정 예약의 월 · 지역 · 활동별 건수 · 일수 · 금액. 최대 36개월.")
    @GetMapping("/statistics/monthly")
    public ApiResult<MonthlyStatisticsResponse> getMonthlyStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        return ApiResult.ok(privacyService.getMonthlyStatistics(from, to));
    }
}
