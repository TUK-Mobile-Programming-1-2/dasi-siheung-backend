package com.dasisiheung.exception;

import lombok.Getter;

/**
 * 서비스 계층에서 비즈니스 규칙 위반을 알릴 때 던진다. 응답 상태 코드는 {@link ErrorCode} 가 결정한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
