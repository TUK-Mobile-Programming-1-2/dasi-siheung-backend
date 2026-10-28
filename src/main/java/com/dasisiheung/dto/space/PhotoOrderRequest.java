package com.dasisiheung.dto.space;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PhotoOrderRequest(
        @Schema(description = "이 공간의 모든 사진 ID 를 원하는 순서대로 (첫 번째가 대표 사진)", example = "[3, 1, 2]")
        @NotEmpty List<@NotNull Long> photoIds
) {
}
