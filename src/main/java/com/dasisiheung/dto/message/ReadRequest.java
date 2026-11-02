package com.dasisiheung.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 읽음 처리. 화면에 보인 마지막 메시지 ID 를 보낸다. (뒤로 돌아가지 않는다) */
public record ReadRequest(
        @Schema(description = "마지막으로 읽은 메시지 ID", example = "42")
        @NotNull @Min(1) Long lastMessageId
) {
}
