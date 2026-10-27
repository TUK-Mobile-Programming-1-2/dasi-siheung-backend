package com.dasisiheung.dto.auth;

import com.dasisiheung.dto.common.ValidationPatterns;
import com.dasisiheung.entity.member.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "이메일 (로그인 아이디)", example = "user@example.com")
        @NotBlank @Email @Size(max = 254)
        String email,

        @Schema(description = "비밀번호 (영문·숫자 포함 8~64자)", example = "password123")
        @NotBlank @Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationPatterns.PASSWORD_MESSAGE)
        String password,

        @Schema(description = "이름", example = "김시흥")
        @NotBlank @Size(max = 30)
        String name,

        @Schema(description = "휴대폰 번호", example = "010-1234-5678")
        @NotBlank @Pattern(regexp = ValidationPatterns.MOBILE_PHONE, message = ValidationPatterns.MOBILE_PHONE_MESSAGE)
        String phone,

        @Schema(description = "역할 (USER: 이용자, PROVIDER: 공간 제공자). ADMIN 은 가입할 수 없음", example = "USER")
        @NotNull
        Role role
) {
}
