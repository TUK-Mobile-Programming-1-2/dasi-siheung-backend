package com.dasisiheung.dto.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @Schema(description = "본인 확인용 현재 비밀번호", example = "password123") @NotBlank String password
) {
}
