package com.dasisiheung.dto.member;

import com.dasisiheung.dto.common.ValidationPatterns;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 바꿀 값만 보낸다. 보내지 않은(null) 항목은 그대로 유지된다. */
public record UpdateProfileRequest(
        @Schema(description = "이름", example = "김시흥")
        @Size(min = 1, max = 30)
        String name,

        @Schema(description = "휴대폰 번호", example = "010-9876-5432")
        @Pattern(regexp = ValidationPatterns.MOBILE_PHONE, message = ValidationPatterns.MOBILE_PHONE_MESSAGE)
        String phone
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "바꿀 항목(name, phone)을 하나 이상 보내야 합니다.")
    public boolean isAnyFieldPresent() {
        return name != null || phone != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "이름은 공백일 수 없습니다.")
    public boolean isNameNotBlank() {
        return name == null || !name.isBlank();
    }
}
