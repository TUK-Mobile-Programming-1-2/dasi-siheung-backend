package com.dasisiheung.service;

import com.dasisiheung.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 프로필 사진 주소. 내 정보 · 글쓴이 · 대화 상대처럼 회원을 보여주는 응답이 함께 쓴다.
 * 공개 버킷이라 주소는 만료되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageResolver {

    private final FileUploadService fileUploadService;

    /** @return 사진이 없거나 탈퇴한 회원이면 null (앱은 기본 이미지를 보여준다) */
    public String urlOf(Member member) {
        if (member.isWithdrawn() || member.getProfileImagePath() == null) {
            return null;
        }
        return fileUploadService.url(UploadPolicy.PROFILE_IMAGE, member.getProfileImagePath());
    }
}
