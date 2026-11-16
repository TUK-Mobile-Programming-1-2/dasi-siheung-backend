package com.dasisiheung.service;

import com.dasisiheung.entity.inquiry.InquiryCategory;

/**
 * 관리자가 1:1 문의에 답변했다. 커밋된 뒤 문의한 회원에게 푸시를 보낸다. ({@link PushNotificationService})
 *
 * @param memberId 받는 사람 (문의한 회원)
 */
public record InquiryAnsweredEvent(
        Long inquiryId,
        Long memberId,
        InquiryCategory category
) {
}
