package com.dasisiheung.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 클라이언트가 분기할 수 있도록 응답의 {@code error.code} 로 enum 이름을 그대로 내려준다.
 * 새 오류는 기능 묶음별 구역에 추가하고, docs/FRONTEND_GUIDE.md 의 오류 코드 표도 함께 갱신한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌했습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 인증 · 회원
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    ROLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "가입할 수 없는 역할입니다. 이용자(USER) 또는 공간 제공자(PROVIDER)만 선택할 수 있습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다. 토큰을 재발급해 주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 정지된 계정입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 현재 비밀번호와 같습니다."),

    // 파일
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "파일을 선택해 주세요."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "올릴 수 없는 파일 형식입니다."),
    STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "파일 저장소에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // 공간
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "공간을 찾을 수 없습니다."),
    SPACE_OUT_OF_SERVICE_AREA(HttpStatus.BAD_REQUEST, "정왕동·거북섬·배곧 지역의 공간만 등록할 수 있습니다."),
    INVALID_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "사용 가능 날짜는 오늘부터 1년 이내로만 지정할 수 있습니다."),
    NOT_SPACE_OWNER(HttpStatus.FORBIDDEN, "본인이 등록한 공간만 관리할 수 있습니다."),
    SPACE_NOT_EDITABLE(HttpStatus.CONFLICT, "숨김 처리된 공간은 수정할 수 없습니다. 관리자에게 문의해 주세요."),
    SPACE_PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "사진은 최대 10장까지 올릴 수 있습니다."),
    SPACE_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다."),
    INVALID_PHOTO_ORDER(HttpStatus.BAD_REQUEST, "이 공간의 모든 사진 ID 를 한 번씩 보내야 합니다."),
    SPACE_PROOF_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "증빙은 최대 5개까지 올릴 수 있습니다."),
    SPACE_PROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "증빙을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
