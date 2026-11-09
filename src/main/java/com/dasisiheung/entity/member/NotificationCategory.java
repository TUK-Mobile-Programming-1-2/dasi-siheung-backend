package com.dasisiheung.entity.member;

/** 회원이 따로 켜고 끌 수 있는 알림 분류. 푸시를 보낼 때 어느 설정을 볼지 정한다. */
public enum NotificationCategory {

    /** 1:1 대화 새 메시지 */
    CHAT,
    /** 신청 · 가격 협의 · 승인 · 거절 등 내 신청과 예약의 진행 상황 */
    ACTIVITY,
    /** 찜한 공간의 소식 */
    FAVORITE_SPACE,
    /** 광고성 정보 (이벤트 · 혜택). 수신 동의한 회원에게만 보낸다 */
    MARKETING
}
