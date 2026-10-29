-- 같은 공간의 확정(APPROVED) 신청끼리 기간이 겹치지 못하게 DB 가 마지막으로 막는다.
-- 애플리케이션은 공간 행 잠금 + 겹침 검사로 먼저 막고, 이 제약은 그 검사를 우회한 경우의 안전장치다.
-- 위반 시 SQLSTATE 23P01 (exclusion_violation) → 409 PERIOD_ALREADY_BOOKED
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE space_application
    ADD CONSTRAINT ex_space_application_no_double_booking
        EXCLUDE USING gist (space_id WITH =, daterange(start_date, end_date, '[]') WITH &&)
        WHERE (status = 'APPROVED');
