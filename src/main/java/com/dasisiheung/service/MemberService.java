package com.dasisiheung.service;

import com.dasisiheung.dto.member.ChangePasswordRequest;
import com.dasisiheung.dto.member.MyProfileResponse;
import com.dasisiheung.dto.member.UpdateProfileRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 내 정보 조회 · 수정 · 비밀번호 변경 · 탈퇴. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public MyProfileResponse getMyProfile(Long memberId) {
        return MyProfileResponse.from(getMember(memberId));
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long memberId, UpdateProfileRequest request) {
        Member member = getMember(memberId);
        member.updateProfile(request.name(), request.phone());
        return MyProfileResponse.from(member);
    }

    /** 비밀번호를 바꾸면 모든 기기의 리프레시 토큰을 폐기한다. (다른 기기는 다시 로그인) */
    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = getMember(memberId);
        verifyPassword(member, request.currentPassword());
        if (passwordEncoder.matches(request.newPassword(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }

    @Transactional
    public void withdraw(Long memberId, String password) {
        Member member = getMember(memberId);
        verifyPassword(member, password);
        member.withdraw(LocalDateTime.now(clock));
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }

    /** 탈퇴한 회원은 없는 회원으로 취급한다. */
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> !member.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void verifyPassword(Member member, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }
}
