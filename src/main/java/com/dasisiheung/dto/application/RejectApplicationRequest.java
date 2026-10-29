package com.dasisiheung.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 신청 거절. 사유는 신청자에게 그대로 보인다. */
public record RejectApplicationRequest(
        @Schema(description = "거절 사유 (필수, 최대 500자)", example = "해당 기간에 내부 공사가 잡혀 있습니다.")
        @NotBlank @Size(max = 500) String reason
) {
}
