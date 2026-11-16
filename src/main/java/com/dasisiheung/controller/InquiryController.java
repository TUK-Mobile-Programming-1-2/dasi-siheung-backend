package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.inquiry.InquiryRequest;
import com.dasisiheung.dto.inquiry.InquiryResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 1:1 문의. 이용자 · 공간 제공자가 같은 API 를 쓴다. */
@Tag(name = "Inquiry", description = "1:1 문의")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "1:1 문의 등록",
            description = "분류 · 내용(최대 1000자)으로 등록합니다. 사진은 응답의 id 로 '문의 사진 올리기'를 따로 호출하세요.")
    @PostMapping("/api/v1/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<InquiryResponse> create(@LoginMember AuthMember loginMember,
                                             @Valid @RequestBody InquiryRequest request) {
        return ApiResult.ok(inquiryService.create(loginMember.id(), request));
    }

    @Operation(summary = "1:1 문의 사진 올리기",
            description = "multipart 파트: files(사진 여러 장). 문의당 최대 3장, JPG·PNG·WEBP 10MB. "
                    + "답변이 끝난 문의에는 올릴 수 없습니다. 응답은 이 문의의 전체 사진이며 임시 주소는 10분 뒤 만료됩니다.")
    @PostMapping(path = "/api/v1/inquiries/{inquiryId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<InquiryResponse> uploadPhotos(@LoginMember AuthMember loginMember,
                                                   @PathVariable Long inquiryId,
                                                   @RequestPart("files") List<MultipartFile> files) {
        return ApiResult.ok(inquiryService.uploadPhotos(loginMember.id(), inquiryId, files));
    }

    @Operation(summary = "내 1:1 문의 내역", description = "내가 남긴 문의, 최근 순. 사진은 장수(photoCount)만 주고 photos 는 null 입니다.")
    @GetMapping("/api/v1/members/me/inquiries")
    public ApiResult<PageResponse<InquiryResponse>> getMyInquiries(@LoginMember AuthMember loginMember,
                                                                   @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(inquiryService.getMyInquiries(loginMember.id(), pageQuery));
    }

    @Operation(summary = "1:1 문의 상세", description = "본인 문의만 볼 수 있습니다. 사진 임시 주소는 10분 뒤 만료됩니다.")
    @GetMapping("/api/v1/inquiries/{inquiryId}")
    public ApiResult<InquiryResponse> getInquiry(@LoginMember AuthMember loginMember, @PathVariable Long inquiryId) {
        return ApiResult.ok(inquiryService.getInquiry(loginMember.id(), inquiryId));
    }
}
