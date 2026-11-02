package com.dasisiheung.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 공간 제공자의 후기 답글. */
public record ReviewReplyRequest(
        @Schema(description = "답글 (최대 500자)", example = "깨끗하게 써 주셔서 감사합니다. 또 찾아 주세요!")
        @NotBlank @Size(max = 500) String content
) {
}
