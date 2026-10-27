package com.dasisiheung.dto.member;

import com.dasisiheung.dto.common.ValidationPatterns;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호", example = "password123") @NotBlank String currentPassword,

        @Schema(description = "새 비밀번호 (영문·숫자 포함 8~64자)", example = "newPassword456")
        @NotBlank @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MESSAGE)
        String newPassword
) {
}
