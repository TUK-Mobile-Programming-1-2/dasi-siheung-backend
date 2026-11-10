package com.dasisiheung.dto.faq;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FaqOrderRequest(
        @Schema(description = "모든 자주 묻는 질문 ID 를 보여줄 순서대로 (첫 번째가 맨 위)", example = "[3, 1, 2]")
        @NotEmpty List<@NotNull Long> faqIds
) {
}
