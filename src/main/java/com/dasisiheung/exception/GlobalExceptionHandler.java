package com.dasisiheung.exception;

import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.ErrorResponse;
import com.dasisiheung.dto.common.ErrorResponse.FieldErrorDetail;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException e) {
        log.debug("비즈니스 예외: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return toResponse(errorCode, errorCode.toErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleInvalidRequestBody(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return toInvalidInputResponse(fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResult<Void>> handleInvalidParameter(HandlerMethodValidationException e) {
        List<FieldErrorDetail> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(GlobalExceptionHandler::toFieldErrors)
                .toList();
        return toInvalidInputResponse(fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResult<Void>> handleMalformedRequest(Exception e) {
        log.debug("잘못된 요청 형식: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(NoResourceFoundException e) {
        return toResponse(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 인증·인가 예외는 다시 던져 Spring Security 의 ExceptionTranslationFilter 가
     * 미인증(401)과 권한 부족(403)을 구분해 처리하게 한다.
     */
    @ExceptionHandler({AuthenticationException.class, AccessDeniedException.class})
    public void delegateToSecurityFilter(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private static Stream<FieldErrorDetail> toFieldErrors(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return result.getResolvableErrors().stream()
                .map(error -> new FieldErrorDetail(parameterName, error.getDefaultMessage()));
    }

    private ResponseEntity<ApiResult<Void>> toInvalidInputResponse(List<FieldErrorDetail> fieldErrors) {
        return toResponse(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.toErrorResponse(fieldErrors));
    }

    private ResponseEntity<ApiResult<Void>> toResponse(ErrorCode errorCode) {
        return toResponse(errorCode, errorCode.toErrorResponse());
    }

    private ResponseEntity<ApiResult<Void>> toResponse(ErrorCode errorCode, ErrorResponse errorResponse) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiResult.fail(errorResponse));
    }
}
