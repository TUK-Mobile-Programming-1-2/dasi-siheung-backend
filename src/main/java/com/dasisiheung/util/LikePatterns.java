package com.dasisiheung.util;

import java.util.Locale;

/**
 * LIKE 검색 패턴. 사용자가 입력한 % _ \ 는 글자 그대로 찾도록 이스케이프한다.
 * 쿼리에는 {@code like :pattern escape '\'} 처럼 {@link #ESCAPE} 를 함께 쓴다.
 */
public final class LikePatterns {

    public static final char ESCAPE = '\\';

    private LikePatterns() {
    }

    /** 대소문자 무시 "포함" 패턴. 쿼리 쪽도 lower() 로 비교해야 한다. */
    public static String containsIgnoreCase(String keyword) {
        String escaped = keyword.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
