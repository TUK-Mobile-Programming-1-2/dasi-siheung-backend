package com.dasisiheung.security;

import com.dasisiheung.exception.ErrorCode;
import lombok.Getter;

/** 액세스 토큰이 만료되었거나 위조·손상된 경우. */
@Getter
public class InvalidJwtException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public InvalidJwtException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
