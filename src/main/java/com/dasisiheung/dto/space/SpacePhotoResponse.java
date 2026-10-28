package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.SpacePhoto;
import io.swagger.v3.oas.annotations.media.Schema;

public record SpacePhotoResponse(
        @Schema(description = "사진 ID", example = "1") Long id,
        @Schema(description = "사진 주소 (공개, 고정)") String url,
        @Schema(description = "순서 (0 이 대표 사진)", example = "0") int sortOrder
) {

    public static SpacePhotoResponse of(SpacePhoto photo, String url) {
        return new SpacePhotoResponse(photo.getId(), url, photo.getSortOrder());
    }
}
