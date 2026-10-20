package com.dasisiheung.exception;

import com.dasisiheung.dto.common.ErrorResponse;
import com.dasisiheung.dto.common.ErrorResponse.FieldErrorDetail;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 클라이언트가 분기할 수 있도록 응답의 {@code error.code} 로 enum 이름을 그대로 내려준다.
 * 도메인 오류(예: 예약 기간 중복)는 기능 개발 시 이곳에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    public ErrorResponse toErrorResponse() {
        return toErrorResponse(message);
    }

    public ErrorResponse toErrorResponse(String detailMessage) {
        return ErrorResponse.of(name(), detailMessage);
    }

    public ErrorResponse toErrorResponse(List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(name(), message, fieldErrors);
    }
}
