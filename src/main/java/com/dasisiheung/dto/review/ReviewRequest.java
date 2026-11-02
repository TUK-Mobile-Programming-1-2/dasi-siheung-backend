package com.dasisiheung.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 이용 후기 작성 · 수정. */
public record ReviewRequest(
        @Schema(description = "평점 (1~5)", example = "5")
        @NotNull @Min(1) @Max(5) Integer rating,

        @Schema(description = "후기 (최대 1000자)", example = "역 앞이라 유동 인구가 많아 첫날부터 매출이 좋았어요.")
        @NotBlank @Size(max = 1000) String content
) {
}
