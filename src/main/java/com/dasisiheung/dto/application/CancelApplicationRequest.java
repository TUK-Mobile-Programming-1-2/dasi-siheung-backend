package com.dasisiheung.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** 신청 취소. 사유는 선택이며 공간 제공자에게 보인다. */
public record CancelApplicationRequest(
        @Schema(description = "취소 사유 (선택, 최대 500자)", example = "일정이 바뀌었습니다.")
        @Size(max = 500) String reason
) {
}
