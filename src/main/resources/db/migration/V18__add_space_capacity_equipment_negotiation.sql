-- 공간 수용 인원 · 비품 · 가격 협의 가능 여부.
-- 1) max_capacity: 동시에 머물 수 있는 최대 인원. 기존 공간은 비워 둔다(NULL = 정하지 않음, 인원 제한 없음).
-- 2) price_negotiable: 이용자가 가격을 제안할 수 있는지. 기존 공간은 지금처럼 협의 가능(TRUE).
-- 3) space_equipment: 빌려주는 비품(이름 · 수량). 보유 시설(space_facility, 고정 목록)과 달리 제공자가 자유롭게 적는다.
ALTER TABLE space
    ADD COLUMN max_capacity     INTEGER,
    ADD COLUMN price_negotiable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT ck_space_max_capacity CHECK (max_capacity BETWEEN 1 AND 500);

CREATE TABLE space_equipment
(
    space_id   BIGINT      NOT NULL REFERENCES space (id) ON DELETE CASCADE,
    sort_order INTEGER     NOT NULL,
    name       VARCHAR(40) NOT NULL,
    quantity   INTEGER     NOT NULL,
    PRIMARY KEY (space_id, sort_order),
    CONSTRAINT ck_space_equipment_quantity CHECK (quantity BETWEEN 1 AND 999)
);

COMMENT ON COLUMN space.max_capacity IS '최대 수용 인원 (NULL 이면 정하지 않음 — 신청 인원 제한 없음)';
COMMENT ON COLUMN space.price_negotiable IS '이용자 · 제공자가 가격을 제안할 수 있는지 (FALSE 면 정가로만 신청)';
COMMENT ON TABLE space_equipment IS '공간 제공자가 빌려주는 비품 (이름 · 수량, 입력 순서대로)';
