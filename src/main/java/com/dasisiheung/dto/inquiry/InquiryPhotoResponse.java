package com.dasisiheung.dto.inquiry;

import com.dasisiheung.entity.inquiry.InquiryPhoto;
import io.swagger.v3.oas.annotations.media.Schema;

/** 문의 첨부 사진. 주소는 10분짜리 임시 주소다. */
public record InquiryPhotoResponse(
        @Schema(description = "사진 ID", example = "1") Long id,
        @Schema(description = "열람용 임시 주소 (전체 URL, 10분 뒤 만료)") String url
) {

    public static InquiryPhotoResponse of(InquiryPhoto photo, String url) {
        return new InquiryPhotoResponse(photo.getId(), url);
    }
}
