package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 공간 제공자(PROVIDER) 전용. /api/v1/provider/** 는 SecurityConfig 에서 역할을 검사한다. */
@Tag(name = "Provider Space", description = "공간 제공자 — 내 공간 등록 · 관리")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/provider/spaces")
@RequiredArgsConstructor
public class ProviderSpaceController {

    private final SpaceService spaceService;

    @Operation(summary = "공간 등록",
            description = "지역은 좌표로 판정합니다. 등록하면 관리자 검수 대기(PENDING_REVIEW) 상태가 되고, 사진·증빙은 등록 후 따로 올립니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<ProviderSpaceResponse> register(@LoginMember AuthMember loginMember,
                                                     @Valid @RequestBody SpaceRequest request) {
        return ApiResult.ok(spaceService.register(loginMember.id(), request));
    }
}
