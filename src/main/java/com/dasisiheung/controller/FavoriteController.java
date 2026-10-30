package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.FavoriteResponse;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이용자(USER)의 공간 찜. */
@Tag(name = "Favorite", description = "공간 찜")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "찜하기", description = "이미 찜했으면 그대로 둡니다. 공개 중인 공간만 찜할 수 있습니다.")
    @PutMapping("/api/v1/spaces/{spaceId}/favorite")
    public ApiResult<FavoriteResponse> add(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(favoriteService.add(loginMember.id(), spaceId));
    }

    @Operation(summary = "찜 해제", description = "찜하지 않았어도 성공합니다.")
    @DeleteMapping("/api/v1/spaces/{spaceId}/favorite")
    public ApiResult<FavoriteResponse> remove(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(favoriteService.remove(loginMember.id(), spaceId));
    }

    @Operation(summary = "내 찜 목록", description = "공개 중인 공간만, 최근 찜한 순.")
    @GetMapping("/api/v1/members/me/favorites")
    public ApiResult<PageResponse<SpaceSummaryResponse>> getMyFavorites(@LoginMember AuthMember loginMember,
                                                                        @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(favoriteService.getMyFavorites(loginMember.id(), pageQuery));
    }
}
