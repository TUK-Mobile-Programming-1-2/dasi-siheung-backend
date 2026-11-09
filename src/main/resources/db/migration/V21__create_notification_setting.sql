-- 회원별 알림 설정. 한 번도 저장하지 않은 회원은 행이 없고, 조회하면 기본값을 내려준다.
-- 탈퇴하면 행을 지운다. (회원 행은 비식별화로 남으므로 CASCADE 는 하드 삭제에만 동작)
CREATE TABLE notification_setting
(
    member_id              BIGINT PRIMARY KEY REFERENCES member (id) ON DELETE CASCADE,
    push_enabled           BOOLEAN   NOT NULL,
    chat_enabled           BOOLEAN   NOT NULL,
    activity_enabled       BOOLEAN   NOT NULL,
    favorite_space_enabled BOOLEAN   NOT NULL,
    marketing_enabled      BOOLEAN   NOT NULL,
    marketing_consented_at TIMESTAMP,
    dnd_enabled            BOOLEAN   NOT NULL,
    dnd_start_hour         SMALLINT  NOT NULL,
    dnd_end_hour           SMALLINT  NOT NULL,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL,
    CONSTRAINT ck_notification_setting_dnd_start_hour CHECK (dnd_start_hour BETWEEN 0 AND 23),
    CONSTRAINT ck_notification_setting_dnd_end_hour CHECK (dnd_end_hour BETWEEN 0 AND 23),
    CONSTRAINT ck_notification_setting_dnd_period CHECK (NOT dnd_enabled OR dnd_start_hour <> dnd_end_hour),
    CONSTRAINT ck_notification_setting_marketing_consent
        CHECK (marketing_enabled = (marketing_consented_at IS NOT NULL))
);

COMMENT ON TABLE notification_setting IS '회원별 알림 설정 (행이 없으면 기본값)';
COMMENT ON COLUMN notification_setting.push_enabled IS '푸시 알림 전체 스위치 — false 면 분류별 설정과 관계없이 보내지 않는다';
COMMENT ON COLUMN notification_setting.marketing_consented_at IS '광고성 정보 수신 동의 시각 (동의 중일 때만 값, 철회하면 NULL)';
COMMENT ON COLUMN notification_setting.dnd_start_hour IS '방해금지 시작 시각(시, 포함). 끝 시각보다 크면 자정을 넘는 구간';
COMMENT ON COLUMN notification_setting.dnd_end_hour IS '방해금지 끝 시각(시, 미포함)';
