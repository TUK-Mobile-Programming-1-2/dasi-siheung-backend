package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.review.ReviewReplyRequest;
import com.dasisiheung.dto.review.ReviewRequest;
import com.dasisiheung.dto.review.ReviewResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 이용 후기. 공간별 후기 조회는 로그인 없이, 작성 · 수정 · 삭제는 신청자, 답글은 공간 제공자. */
@Tag(name = "Review", description = "이용 후기 · 평점")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "공간 후기 목록", description = "공개 공간의 후기 (최근 순). 로그인 없이 볼 수 있습니다.")
    @GetMapping("/api/v1/spaces/{spaceId}/reviews")
    public ApiResult<PageResponse<ReviewResponse>> getSpaceReviews(@PathVariable Long spaceId,
                                                                   @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(reviewService.getSpaceReviews(spaceId, pageQuery));
    }

    @Operation(summary = "후기 작성", description = "이용이 끝난(종료일이 지난) 확정 예약의 신청자만, 예약 1건당 1번.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/api/v1/applications/{applicationId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public ApiResult<ReviewResponse> write(@LoginMember AuthMember loginMember, @PathVariable Long applicationId,
                                           @Valid @RequestBody ReviewRequest request) {
        return ApiResult.ok(reviewService.write(loginMember.id(), applicationId, request));
    }

    @Operation(summary = "후기 수정")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PutMapping("/api/v1/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<ReviewResponse> edit(@LoginMember AuthMember loginMember, @PathVariable Long reviewId,
                                          @Valid @RequestBody ReviewRequest request) {
        return ApiResult.ok(reviewService.edit(loginMember.id(), reviewId, request));
    }

    @Operation(summary = "후기 삭제")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @DeleteMapping("/api/v1/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<Void> delete(@LoginMember AuthMember loginMember, @PathVariable Long reviewId) {
        reviewService.delete(loginMember.id(), reviewId);
        return ApiResult.ok();
    }

    @Operation(summary = "후기 답글 (공간 제공자)", description = "내 공간의 후기에만 답할 수 있고, 다시 보내면 고칩니다.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @PostMapping("/api/v1/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('PROVIDER')")
    public ApiResult<ReviewResponse> reply(@LoginMember AuthMember loginMember, @PathVariable Long reviewId,
                                           @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResult.ok(reviewService.reply(loginMember.id(), reviewId, request));
    }

    @Operation(summary = "내가 쓴 후기")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/api/v1/members/me/reviews")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<PageResponse<ReviewResponse>> getMyReviews(@LoginMember AuthMember loginMember,
                                                                @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(reviewService.getMyReviews(loginMember.id(), pageQuery));
    }
}
