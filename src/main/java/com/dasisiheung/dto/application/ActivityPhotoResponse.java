package com.dasisiheung.dto.application;

import com.dasisiheung.entity.application.ActivityPhoto;
import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.entity.application.PhotoPhase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 활동 전후 사진. 주소는 10분짜리 임시 주소다. */
public record ActivityPhotoResponse(
        @Schema(description = "사진 ID", example = "1") Long id,
        @Schema(description = "시점", example = "BEFORE") PhotoPhase phase,
        @Schema(description = "올린 쪽", example = "APPLICANT") OfferSide uploaderSide,
        @Schema(description = "올린 회원 이름") String uploaderName,
        @Schema(description = "열람용 임시 주소 (10분 뒤 만료)") String url,
        @Schema(description = "올린 시각") LocalDateTime createdAt
) {

    public static ActivityPhotoResponse of(ActivityPhoto photo, String url) {
        return new ActivityPhotoResponse(photo.getId(), photo.getPhase(), photo.getUploaderSide(),
                photo.getUploader().getName(), url, photo.getCreatedAt());
    }
}
