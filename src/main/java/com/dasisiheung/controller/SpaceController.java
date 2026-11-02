package com.dasisiheung.controller;

import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.dto.space.SpaceMapRequest;
import com.dasisiheung.dto.space.RecommendationResponse;
import com.dasisiheung.dto.space.SpaceMapResponse;
import com.dasisiheung.dto.space.SpaceSearchRequest;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.UsePurpose;
import com.dasisiheung.service.RecommendationService;
import com.dasisiheung.service.SpaceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공간 둘러보기. 조회(GET)는 로그인 없이 호출할 수 있다. */
@Tag(name = "Space", description = "공간 둘러보기 — 상세 · 검색 · 지도")
@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceQueryService spaceQueryService;
    private final RecommendationService recommendationService;

    @Operation(summary = "공간 검색",
            description = "지역 · 활동 · 이용 목적 · 기간 · 예산 · 시설 · 검색어로 공개 공간을 찾습니다. 모든 조건은 선택입니다.")
    @GetMapping
    public ApiResult<PageResponse<SpaceSummaryResponse>> search(@Valid @ParameterObject SpaceSearchRequest request,
                                                                @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(spaceQueryService.search(request, pageQuery));
    }

    @Operation(summary = "지도 마커",
            description = "지도 화면 영역(남서·북동 꼭짓점) 안의 공개 공간. 최대 300개이며 넘으면 truncated = true 입니다.")
    @GetMapping("/map")
    public ApiResult<SpaceMapResponse> getMarkers(@Valid @ParameterObject SpaceMapRequest request) {
        return ApiResult.ok(spaceQueryService.getMarkers(request));
    }

    @Operation(summary = "공간 추천",
            description = "활동 · 기간은 반드시 맞는 공간만, 이용 목적에 맞는 상권 · 평점 · 찜 · 후기로 점수를 매겨 높은 순으로 줍니다.")
    @GetMapping("/recommendations")
    public ApiResult<List<RecommendationResponse>> recommend(
            @RequestParam(required = false) UsePurpose purpose,
            @RequestParam(required = false) ActivityType activityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        return ApiResult.ok(recommendationService.recommend(purpose, activityType, startDate, endDate, limit));
    }

    @Operation(summary = "공간 상세", description = "공개 중인 공간만 조회됩니다. 사용 가능 날짜는 오늘 이후 · 확정 예약 제외. "
                    + "토큰을 보내면 favorited 에 찜 여부가 담깁니다.")
    @GetMapping("/{spaceId}")
    public ApiResult<SpaceDetailResponse> getSpace(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        Long memberId = loginMember == null ? null : loginMember.id();
        return ApiResult.ok(spaceQueryService.getSpace(spaceId, memberId));
    }
}
