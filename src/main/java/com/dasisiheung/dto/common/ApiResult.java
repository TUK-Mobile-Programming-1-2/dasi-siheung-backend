package com.dasisiheung.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 모든 API 응답의 공통 형식.
 * <pre>
 * 성공: { "success": true,  "data": { ... } }
 * 실패: { "success": false, "error": { "code": "...", "message": "..." } }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
        @Schema(description = "요청 성공 여부", example = "true") boolean success,
        @Schema(description = "성공 시 응답 데이터") T data,
        @Schema(description = "실패 시 오류 정보") ErrorResponse error
) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(true, null, null);
    }

    public static ApiResult<Void> fail(ErrorResponse error) {
        return new ApiResult<>(false, null, error);
    }
}
