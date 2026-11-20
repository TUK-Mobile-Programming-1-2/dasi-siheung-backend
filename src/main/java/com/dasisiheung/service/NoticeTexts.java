package com.dasisiheung.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 푸시 · 알림함 문구에 쓰는 짧은 표기. */
final class NoticeTexts {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d");

    private NoticeTexts() {
    }

    /** 11/20 또는 11/20~11/22 */
    static String period(LocalDate start, LocalDate end) {
        return start.equals(end) ? SHORT_DATE.format(start) : SHORT_DATE.format(start) + "~" + SHORT_DATE.format(end);
    }

    /** 95,000원 */
    static String won(int amount) {
        return "%,d원".formatted(amount);
    }
}
