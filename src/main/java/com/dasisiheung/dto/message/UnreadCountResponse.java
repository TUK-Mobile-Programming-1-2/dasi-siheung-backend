package com.dasisiheung.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadCountResponse(
        @Schema(description = "모든 대화의 안 읽은 메시지 수", example = "3") long count
) {
}
