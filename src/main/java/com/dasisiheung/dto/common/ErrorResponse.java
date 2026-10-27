package com.dasisiheung.dto.common;

import com.dasisiheung.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(description = "오류 코드 (클라이언트 분기용)", example = "INVALID_INPUT") String code,
        @Schema(description = "사용자에게 보여줄 수 있는 오류 메시지", example = "요청 값이 올바르지 않습니다.") String message,
        @Schema(description = "입력값 검증 실패 목록") List<FieldErrorDetail> fieldErrors
) {

    public ErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }

    public record FieldErrorDetail(
            @Schema(description = "검증에 실패한 필드", example = "name") String field,
            @Schema(description = "실패 사유", example = "공백일 수 없습니다") String reason
    ) {
    }
}
