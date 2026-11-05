package com.dasisiheung.dto.report;

import com.dasisiheung.entity.report.ReportPhoto;
import io.swagger.v3.oas.annotations.media.Schema;

/** 신고 증거 사진. 주소는 10분짜리 임시 주소다. */
public record ReportPhotoResponse(
        @Schema(description = "사진 ID", example = "1") Long id,
        @Schema(description = "열람용 임시 주소 (10분 뒤 만료)") String url
) {

    public static ReportPhotoResponse of(ReportPhoto photo, String url) {
        return new ReportPhotoResponse(photo.getId(), url);
    }
}
