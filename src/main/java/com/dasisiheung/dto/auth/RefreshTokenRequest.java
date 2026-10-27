package com.dasisiheung.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 토큰 재발급·로그아웃 요청. */
public record RefreshTokenRequest(
        @Schema(description = "로그인 때 받은 리프레시 토큰") @NotBlank String refreshToken
) {
}
