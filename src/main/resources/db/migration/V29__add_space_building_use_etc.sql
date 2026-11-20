-- 건축물 용도 '기타' 직접 입력 (Android #112).
-- 1) building_use_etc: 용도가 기타(ETC)일 때 제공자가 적은 용도 (최대 50자). 다른 용도면 항상 NULL.
--    이 컬럼 전에 등록한 기타 공간은 NULL 이며, 다음 수정 때 앱이 입력을 받는다.
ALTER TABLE space
    ADD COLUMN building_use_etc VARCHAR(50),
    ADD CONSTRAINT ck_space_building_use_etc CHECK (building_use = 'ETC' OR building_use_etc IS NULL);

-- 2) 앱이 임시로 소개(description) 맨 앞에 붙여 보내던 "[건축물 용도(기타): <직접 입력>]" 한 줄을 새 컬럼으로 옮기고 소개에서 뗀다.
--    기타 공간이고, 직접 입력이 비어 있지 않고, 떼고 남은 소개가 비어 있지 않을 때만 옮긴다.
WITH legacy AS (
    SELECT id,
           btrim((regexp_match(description, '^\[건축물 용도\(기타\): ([^]\n]*)\]\r?\n'))[1]) AS etc,
           regexp_replace(description, '^\[건축물 용도\(기타\): [^]\n]*\]\r?\n', '') AS rest
    FROM space
    WHERE building_use = 'ETC'
      AND building_use_etc IS NULL
      AND description ~ '^\[건축물 용도\(기타\): [^]\n]*\]\r?\n'
)
UPDATE space s
SET building_use_etc = left(legacy.etc, 50),
    description      = btrim(legacy.rest, E' \t\r\n')
FROM legacy
WHERE s.id = legacy.id
  AND legacy.etc <> ''
  AND btrim(legacy.rest, E' \t\r\n') <> '';

COMMENT ON COLUMN space.building_use_etc IS '건축물 용도가 기타(ETC)일 때 직접 입력한 용도 (다른 용도면 NULL)';
