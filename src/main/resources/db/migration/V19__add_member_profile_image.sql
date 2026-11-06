-- 회원 프로필 사진. 공개 버킷에 두고 저장소 안의 경로만 저장한다. (주소는 조회할 때 만든다)
-- 사진을 바꾸거나 지우면 이전 파일은 저장소에서 지우고, 탈퇴하면 경로도 비운다.
ALTER TABLE member
    ADD COLUMN profile_image_path VARCHAR(300);

COMMENT ON COLUMN member.profile_image_path IS '프로필 사진 저장 경로 (공개 버킷, NULL 이면 기본 이미지)';
