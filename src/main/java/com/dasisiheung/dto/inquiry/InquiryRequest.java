package com.dasisiheung.dto.inquiry;

import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 1:1 문의 등록. 사진은 등록한 뒤 POST /api/v1/inquiries/{inquiryId}/photos 로 따로 올린다. */
public record InquiryRequest(
        @Schema(description = "분류 (선택지 inquiryCategories)", example = "ACCOUNT")
        @NotNull InquiryCategory category,

        @Schema(description = "문의 내용 (최대 1000자)", example = "비밀번호를 바꿨는데 로그인이 안 돼요.")
        @NotBlank @Size(max = Inquiry.MAX_CONTENT_LENGTH) String content
) {
}
