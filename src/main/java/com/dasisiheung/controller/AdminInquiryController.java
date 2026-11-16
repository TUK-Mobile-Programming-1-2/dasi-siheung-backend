package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.inquiry.AdminInquiryResponse;
import com.dasisiheung.dto.inquiry.AnswerInquiryRequest;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.AdminInquiryService;
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

/** 관리자(ADMIN) 1:1 문의 답변. */
@Tag(name = "Admin Inquiry", description = "관리자 — 1:1 문의 답변")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @Operation(summary = "1:1 문의 목록", description = "status=WAITING 이면 오래된 순, 그 밖은 최근 순.")
    @GetMapping
    public ApiResult<PageResponse<AdminInquiryResponse>> getInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(adminInquiryService.getInquiries(status, pageQuery));
    }

    @Operation(summary = "1:1 문의 상세")
    @GetMapping("/{inquiryId}")
    public ApiResult<AdminInquiryResponse> getInquiry(@PathVariable Long inquiryId) {
        return ApiResult.ok(adminInquiryService.getInquiry(inquiryId));
    }

    @Operation(summary = "1:1 문의 답변",
            description = "답변은 한 번만 등록할 수 있습니다. 등록하면 문의한 회원에게 푸시(INQUIRY_ANSWERED)를 보냅니다.")
    @PostMapping("/{inquiryId}/answer")
    public ApiResult<AdminInquiryResponse> answer(@LoginMember AuthMember loginMember, @PathVariable Long inquiryId,
                                                  @Valid @RequestBody AnswerInquiryRequest request) {
        return ApiResult.ok(adminInquiryService.answer(loginMember.id(), inquiryId, request));
    }
}
