package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.admin.AdminSpaceDetailResponse;
import com.dasisiheung.dto.admin.AdminSpaceSummaryResponse;
import com.dasisiheung.dto.admin.SpaceReviewRequest;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.AdminSpaceService;
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

/** 관리자(ADMIN) 전용. /api/v1/admin/** 는 SecurityConfig 에서 역할을 검사한다. */
@Tag(name = "Admin Space", description = "관리자 — 공간 검수 · 현황")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/spaces")
@RequiredArgsConstructor
public class AdminSpaceController {

    private final AdminSpaceService adminSpaceService;

    @Operation(summary = "공간 목록 (검수 대기 · 전체 현황)",
            description = "status=PENDING_REVIEW 면 검수 대기 목록(오래 기다린 순), 생략하면 전체 공간(최근 순).")
    @GetMapping
    public ApiResult<PageResponse<AdminSpaceSummaryResponse>> getSpaces(
            @RequestParam(required = false) SpaceStatus status, @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(adminSpaceService.getSpaces(status, pageQuery));
    }

    @Operation(summary = "검수 상세", description = "공간 정보, 증빙(10분짜리 임시 주소), 제공자 연락처, 검수 이력, 공식 정보 조회 링크.")
    @GetMapping("/{spaceId}")
    public ApiResult<AdminSpaceDetailResponse> getSpace(@PathVariable Long spaceId) {
        return ApiResult.ok(adminSpaceService.getSpace(spaceId));
    }

    @Operation(summary = "검수 결과 등록",
            description = "공개 승인은 증빙 1개 이상 + 5개 항목 모두 확인이 필요합니다. 수정 요청·공개 보류는 사유가 필수입니다.")
    @PostMapping("/{spaceId}/reviews")
    public ApiResult<AdminSpaceDetailResponse> review(@LoginMember AuthMember loginMember, @PathVariable Long spaceId,
                                                      @Valid @RequestBody SpaceReviewRequest request) {
        return ApiResult.ok(adminSpaceService.review(loginMember.id(), spaceId, request));
    }
}
