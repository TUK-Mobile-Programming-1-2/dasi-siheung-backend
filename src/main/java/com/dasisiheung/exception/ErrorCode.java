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
    WITHDRAWAL_BLOCKED_BY_APPLICATION(HttpStatus.CONFLICT,
            "진행 중인 신청 · 예약이 있어 탈퇴할 수 없습니다. 신청을 취소 · 거절하거나 이용이 끝난 뒤 다시 시도해 주세요."),

    // 파일
    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "파일을 선택해 주세요."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 너무 큽니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "올릴 수 없는 파일 형식입니다."),
    STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "파일 저장소에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // 공간
    SPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "공간을 찾을 수 없습니다."),
    SPACE_OUT_OF_SERVICE_AREA(HttpStatus.BAD_REQUEST, "정왕동·거북섬·배곧 지역의 공간만 등록할 수 있습니다."),
    INVALID_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "사용 가능 날짜는 오늘부터 1년 이내로만 지정할 수 있습니다."),
    INVALID_SPACE_STATUS(HttpStatus.CONFLICT, "현재 공간 상태에서는 할 수 없는 작업입니다."),
    NOT_SPACE_OWNER(HttpStatus.FORBIDDEN, "본인이 등록한 공간만 관리할 수 있습니다."),
    SPACE_NOT_EDITABLE(HttpStatus.CONFLICT, "숨김 처리된 공간은 수정할 수 없습니다. 관리자에게 문의해 주세요."),
    SPACE_PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "사진은 최대 10장까지 올릴 수 있습니다."),
    SPACE_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다."),
    INVALID_PHOTO_ORDER(HttpStatus.BAD_REQUEST, "이 공간의 모든 사진 ID 를 한 번씩 보내야 합니다."),
    SPACE_PROOF_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "증빙은 최대 5개까지 올릴 수 있습니다."),
    SPACE_PROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "증빙을 찾을 수 없습니다."),

    // 공간 둘러보기
    INVALID_SEARCH_PERIOD(HttpStatus.BAD_REQUEST, "검색 기간은 오늘부터 최대 90일까지 지정할 수 있습니다."),

    // 관리자 검수
    SPACE_PROOF_REQUIRED(HttpStatus.BAD_REQUEST, "권한 증빙이 없는 공간은 공개 승인할 수 없습니다. 공개 보류로 처리해 주세요."),
    REVIEW_CHECKLIST_INCOMPLETE(HttpStatus.BAD_REQUEST, "검수 항목 5개를 모두 확인해야 공개 승인할 수 있습니다."),
    REVIEW_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "수정 요청·공개 보류는 사유를 입력해야 합니다."),

    // 공간 이용 신청
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "신청을 찾을 수 없습니다."),
    NOT_APPLICATION_PARTICIPANT(HttpStatus.FORBIDDEN, "본인이 보내거나 받은 신청만 다룰 수 있습니다."),
    ACTIVITY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "이 공간에서 허용하지 않는 활동입니다."),
    HEADCOUNT_EXCEEDS_CAPACITY(HttpStatus.BAD_REQUEST, "운영 인원이 공간의 최대 수용 인원을 넘습니다."),
    INVALID_APPLICATION_PERIOD(HttpStatus.BAD_REQUEST, "이용 기간은 오늘부터 시작해 최대 30일까지 신청할 수 있습니다."),
    PERIOD_NOT_AVAILABLE(HttpStatus.CONFLICT, "선택한 기간에 공간을 사용할 수 없는 날짜가 있습니다."),
    DUPLICATE_APPLICATION(HttpStatus.CONFLICT, "같은 공간 · 같은 기간에 이미 검토 중인 신청이 있습니다."),
    INVALID_APPLICATION_STATUS(HttpStatus.CONFLICT, "현재 신청 상태에서는 할 수 없는 작업입니다."),
    APPLICATION_NOT_CANCELLABLE(HttpStatus.CONFLICT, "이용 시작일부터는 확정된 예약을 취소할 수 없습니다. 공간 제공자와 상의해 주세요."),
    PERIOD_ALREADY_BOOKED(HttpStatus.CONFLICT, "선택한 기간에 이미 확정된 예약이 있습니다."),
    ACTIVITY_PHOTO_NOT_ALLOWED(HttpStatus.CONFLICT, "확정된 예약의 이용 기간 앞뒤에만 활동 사진을 올릴 수 있습니다."),
    ACTIVITY_PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이용 전 · 후 사진은 각각 최대 10장까지 올릴 수 있습니다."),
    ACTIVITY_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "활동 사진을 찾을 수 없습니다."),
    NOT_PHOTO_UPLOADER(HttpStatus.FORBIDDEN, "본인이 올린 사진만 지울 수 있습니다."),
    PRICE_NEGOTIATION_IN_PROGRESS(HttpStatus.CONFLICT, "가격 협의가 끝나지 않았습니다. 받은 제안을 수락하거나 상대 답변을 기다려 주세요."),

    // 가격 협의
    INVALID_OFFER_PRICE(HttpStatus.BAD_REQUEST, "제안 금액은 1원 이상, 정가 합계 이하로 입력해 주세요."),
    OFFER_NOT_ALLOWED(HttpStatus.CONFLICT, "보낸 제안에 대한 상대방의 답변을 기다리는 중입니다."),
    NO_PENDING_OFFER(HttpStatus.CONFLICT, "수락할 가격 제안이 없습니다."),
    PRICE_NEGOTIATION_NOT_ALLOWED(HttpStatus.CONFLICT, "가격 협의를 받지 않는 공간입니다. 정가로 신청해 주세요."),

    // 이용 후기
    REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "이용이 끝난 확정 예약에만 후기를 남길 수 있습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 후기를 남긴 예약입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "후기를 찾을 수 없습니다."),
    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "본인이 쓴 후기만 고치거나 지울 수 있습니다."),

    // 커뮤니티
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "본인이 쓴 글만 고치거나 지울 수 있습니다."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "본인이 쓴 댓글만 지울 수 있습니다."),
    POST_PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "게시글 사진은 최대 5장까지 올릴 수 있습니다."),
    POST_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글 사진을 찾을 수 없습니다."),
    NOTICE_ADMIN_ONLY(HttpStatus.FORBIDDEN, "공지는 관리자만 쓸 수 있습니다."),

    // 1:1 소통
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다."),
    NOT_CONVERSATION_MEMBER(HttpStatus.FORBIDDEN, "본인이 참여한 대화만 볼 수 있습니다."),

    // 자주 묻는 질문
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "자주 묻는 질문을 찾을 수 없습니다."),
    INVALID_FAQ_ORDER(HttpStatus.BAD_REQUEST, "모든 자주 묻는 질문 ID 를 한 번씩 보내야 합니다. 목록을 새로 불러와 주세요."),

    // 신고 · 제재
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "신고할 대상을 찾을 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "이미 신고한 대상입니다. 관리자가 확인 중입니다."),
    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "본인 또는 본인이 쓴 글은 신고할 수 없습니다."),
    REPORT_PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "신고 사진은 최대 5장까지 올릴 수 있습니다."),
    REPORT_ALREADY_RESOLVED(HttpStatus.CONFLICT, "이미 처리된 신고입니다."),
    INVALID_REPORT_ACTION(HttpStatus.BAD_REQUEST, "회원 신고는 숨길 수 없습니다. 이용 정지 또는 기각으로 처리해 주세요."),
    CANNOT_SUSPEND_MEMBER(HttpStatus.CONFLICT, "관리자 · 탈퇴 회원은 이용 정지할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
