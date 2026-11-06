package com.dasisiheung.dto.community;

import com.dasisiheung.entity.community.PostPhoto;
import io.swagger.v3.oas.annotations.media.Schema;

/** 게시글 사진. 공개 주소라 만료되지 않는다. */
public record PostPhotoResponse(
        @Schema(description = "사진 ID", example = "1") Long id,
        @Schema(description = "사진 주소") String url
) {

    public static PostPhotoResponse of(PostPhoto photo, String url) {
        return new PostPhotoResponse(photo.getId(), url);
    }
}
