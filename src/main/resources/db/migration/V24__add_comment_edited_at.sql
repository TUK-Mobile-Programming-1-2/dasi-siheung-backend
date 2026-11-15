-- 댓글 수정. 고친 적이 있으면 앱이 "(수정됨)"을 붙인다.
-- updated_at 은 숨김 처리처럼 작성자가 고치지 않은 변경에도 바뀌므로, 작성자가 고친 시각을 따로 둔다.
ALTER TABLE community_comment
    ADD COLUMN edited_at TIMESTAMP;

COMMENT ON COLUMN community_comment.edited_at IS '작성자가 마지막으로 고친 시각 — 고친 적 없으면 NULL';
