package com.dasisiheung.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @Schema(description = "메시지 (최대 1000자)", example = "안녕하세요! 11월 첫 주말에 팝업 가능할까요?")
        @NotBlank @Size(max = 1000) String content
) {
}
