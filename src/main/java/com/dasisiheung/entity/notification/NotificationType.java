package com.dasisiheung.entity.notification;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림함 항목의 종류. 푸시 data 의 {@code type} 에도 이 이름을 그대로 쓴다.
 * 지금은 모두 신청에 연결된 가격 협의 알림이라 항목마다 {@code applicationId} 가 있다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType implements LabeledEnum {

    /** 상대가 가격을 제안 · 역제안했다. 받은 사람이 수락 · 거절 · 역제안할 차례다. */
    PRICE_OFFER_RECEIVED("가격 제안 도착"),
    /** 내가 보낸 제안을 상대가 수락했다. */
    PRICE_OFFER_ACCEPTED("가격 제안 수락"),
    /** 내가 보낸 제안을 상대가 거절했다. 다시 제안할 수 있다. */
    PRICE_OFFER_REJECTED("가격 제안 거절");

    private final String label;
}
