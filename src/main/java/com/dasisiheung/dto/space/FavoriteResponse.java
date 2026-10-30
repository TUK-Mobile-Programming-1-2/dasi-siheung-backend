package com.dasisiheung.dto.space;

import io.swagger.v3.oas.annotations.media.Schema;

/** 찜 추가 · 해제 결과. 같은 요청을 여러 번 보내도 결과가 같다. */
public record FavoriteResponse(
        @Schema(description = "공간 ID", example = "1") Long spaceId,
        @Schema(description = "지금 찜한 상태인지", example = "true") boolean favorited,
        @Schema(description = "공간의 찜 수", example = "12") int favoriteCount
) {
}
