-- 받은 가격 제안 거절. 신청 자체의 거절 · 취소와 달리 신청은 검토 중으로 남고, 양쪽 모두 다시 제안할 수 있다.
-- 1) 협의 상태에 OFFER_REJECTED(제안 거절)를 더한다. 답을 기다리는 제안이 없으므로 pending_offer_amount 는 NULL 이다.
-- 2) 제안 이력에 거절 시각 · 거절 한마디를 남긴다. 한 제안은 수락 · 거절 중 하나만 된다.
ALTER TABLE space_application
    DROP CONSTRAINT ck_space_application_negotiation,
    ADD CONSTRAINT ck_space_application_negotiation
        CHECK (negotiation_status IN ('NONE', 'WAITING_PROVIDER', 'WAITING_APPLICANT', 'AGREED', 'OFFER_REJECTED'));

ALTER TABLE price_offer
    ADD COLUMN rejected_at       TIMESTAMP,
    ADD COLUMN rejection_message VARCHAR(300),
    ADD CONSTRAINT ck_price_offer_answer CHECK (accepted_at IS NULL OR rejected_at IS NULL),
    ADD CONSTRAINT ck_price_offer_rejection_message CHECK (rejection_message IS NULL OR rejected_at IS NOT NULL);

COMMENT ON COLUMN space_application.negotiation_status IS
    'NONE 정가 | WAITING_PROVIDER 제공자 답변 대기 | WAITING_APPLICANT 신청자 답변 대기 | AGREED 합의 | OFFER_REJECTED 받은 제안 거절(재제안 가능)';
COMMENT ON COLUMN price_offer.rejected_at IS '상대가 거절한 시각 (거절된 제안만)';
COMMENT ON COLUMN price_offer.rejection_message IS '거절 한마디 (선택, 보관 기간이 지나면 비식별화)';
