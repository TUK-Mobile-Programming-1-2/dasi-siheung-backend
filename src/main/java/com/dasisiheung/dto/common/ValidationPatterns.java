package com.dasisiheung.dto.common;

/**
 * 여러 요청 DTO 가 함께 쓰는 입력 형식. 앱·관리자 웹도 같은 규칙으로 미리 검사하면 왕복을 줄일 수 있다.
 */
public final class ValidationPatterns {

    /** 영문과 숫자를 각각 1자 이상 포함한 8~64자. */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 영문과 숫자를 포함해 8~64자여야 합니다.";

    /** 휴대폰 번호. 하이픈은 있어도 되고 없어도 된다. */
    public static final String MOBILE_PHONE = "^01[016789]-?\\d{3,4}-?\\d{4}$";
    public static final String MOBILE_PHONE_MESSAGE = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";

    private ValidationPatterns() {
    }
}
