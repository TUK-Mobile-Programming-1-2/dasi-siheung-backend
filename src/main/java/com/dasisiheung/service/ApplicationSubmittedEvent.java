package com.dasisiheung.service;

import java.time.LocalDate;

/**
 * 이용자가 공간에 이용 신청을 냈다. 커밋된 뒤 공간 제공자에게 푸시를 보낸다. ({@link PushNotificationService})
 *
 * @param providerId 받는 사람 (공간 제공자)
 * @param withOffer  희망 가격 제안을 함께 보냈는지
 */
public record ApplicationSubmittedEvent(
        Long applicationId,
        Long spaceId,
        String spaceTitle,
        Long providerId,
        String applicantName,
        LocalDate startDate,
        LocalDate endDate,
        boolean withOffer
) {
}
