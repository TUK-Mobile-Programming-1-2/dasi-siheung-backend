package com.dasisiheung.dto.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 가격 제안 · 역제안. 금액은 이용 기간 전체 합계다. */
public record PriceOfferRequest(
        @Schema(description = "제안 금액(원, 기간 합계) — 1원 이상 정가 합계 이하", example = "90000")
        @NotNull @Min(1) Integer amount,

        @Schema(description = "한마디 (선택, 최대 300자)", example = "이틀 연속 이용이라 조금만 조정 부탁드립니다.")
        @Size(max = 300) String message
) {
}
