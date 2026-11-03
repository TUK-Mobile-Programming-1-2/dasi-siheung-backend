package com.dasisiheung.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 개인정보 보관 기간 (app.privacy.*). 기간이 지나면 파기 작업이 비식별화 · 삭제한다.
 *
 * @param applicationRetentionDays 신청서 글(활동 이름 · 계획 · 사유)을 이용 종료 뒤 보관하는 날수
 * @param conversationRetentionDays 마지막 메시지 뒤 1:1 대화를 보관하는 날수
 * @param activityPhotoRetentionDays 이용 종료 뒤 활동 사진을 보관하는 날수
 */
@ConfigurationProperties(prefix = "app.privacy")
public record PrivacyProperties(
        Integer applicationRetentionDays,
        Integer conversationRetentionDays,
        Integer activityPhotoRetentionDays
) {

    public PrivacyProperties {
        applicationRetentionDays = applicationRetentionDays == null ? 365 : applicationRetentionDays;
        conversationRetentionDays = conversationRetentionDays == null ? 365 : conversationRetentionDays;
        activityPhotoRetentionDays = activityPhotoRetentionDays == null ? 90 : activityPhotoRetentionDays;
    }
}
