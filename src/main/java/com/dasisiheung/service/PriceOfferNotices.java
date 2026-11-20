package com.dasisiheung.service;

import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.entity.application.PriceOffer;
import com.dasisiheung.entity.notification.NotificationType;

/**
 * 가격 협의 알림의 받는 사람과 문구. 제안은 상대에게, 수락 · 거절은 제안한 사람에게 간다.
 * 한마디(자유 입력)는 문구에 넣지 않는다. (신청 상세의 협의 이력에서 본다)
 */
final class PriceOfferNotices {

    private PriceOfferNotices() {
    }

    /** 제목: 신청자의 가격 제안 / 공간 제공자의 역제안 — 받은 쪽이 수락 · 거절 · 역제안할 차례다. */
    static NotificationDraft received(Application application, PriceOffer offer) {
        OfferSide proposer = offer.getProposerSide();
        String title = proposer == OfferSide.PROVIDER ? "공간 제공자의 역제안" : "신청자의 가격 제안";
        String body = "%s님이 '%s' %s 이용 금액으로 %s을 제안했어요.".formatted(nameOf(application, proposer),
                application.getSpace().getTitle(), period(application), NoticeTexts.won(offer.getAmount()));
        return draft(application, proposer.counterpart(), NotificationType.PRICE_OFFER_RECEIVED, title, body);
    }

    /** 수락한 쪽 → 제안한 쪽. */
    static NotificationDraft accepted(Application application, PriceOffer offer) {
        return answered(application, offer, NotificationType.PRICE_OFFER_ACCEPTED, "가격 제안 수락",
                "%s님이 '%s' %s 이용 금액 %s 제안을 수락했어요.");
    }

    /** 거절한 쪽 → 제안한 쪽. 신청은 검토 중으로 남아 다시 제안할 수 있다. */
    static NotificationDraft rejected(Application application, PriceOffer offer) {
        return answered(application, offer, NotificationType.PRICE_OFFER_REJECTED, "가격 제안 거절",
                "%s님이 '%s' %s 이용 금액 %s 제안을 거절했어요. 다시 제안할 수 있어요.");
    }

    private static NotificationDraft answered(Application application, PriceOffer offer, NotificationType type,
                                              String title, String bodyFormat) {
        OfferSide proposer = offer.getProposerSide();
        String body = bodyFormat.formatted(nameOf(application, proposer.counterpart()),
                application.getSpace().getTitle(), period(application), NoticeTexts.won(offer.getAmount()));
        return draft(application, proposer, type, title, body);
    }

    private static NotificationDraft draft(Application application, OfferSide recipient, NotificationType type,
                                           String title, String body) {
        return new NotificationDraft(application.memberOf(recipient).getId(), type, application.getId(), title, body);
    }

    private static String nameOf(Application application, OfferSide side) {
        return application.memberOf(side).getName();
    }

    private static String period(Application application) {
        return NoticeTexts.period(application.getStartDate(), application.getEndDate());
    }
}
