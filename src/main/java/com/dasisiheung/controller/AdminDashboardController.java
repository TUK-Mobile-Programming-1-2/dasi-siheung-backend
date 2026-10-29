package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.admin.DashboardResponse;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard", description = "관리자 — 운영 현황")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "대시보드 수치", description = "검수 대기, 상태·지역별 공간, 역할별 회원, 최근 등록 공간.")
    @GetMapping
    public ApiResult<DashboardResponse> getDashboard() {
        return ApiResult.ok(adminDashboardService.getDashboard());
    }
}
