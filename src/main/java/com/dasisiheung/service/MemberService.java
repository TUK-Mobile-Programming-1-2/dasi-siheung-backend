package com.dasisiheung.service;

import com.dasisiheung.dto.member.ChangePasswordRequest;
import com.dasisiheung.dto.member.MyProfileResponse;
import com.dasisiheung.dto.member.UpdateProfileRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 내 정보 조회 · 수정 · 프로필 사진 · 비밀번호 변경 · 탈퇴. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final String PROFILE_IMAGE_DIRECTORY = "members/%d/profile";

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final ProfileImageResolver profileImageResolver;
    private final Clock clock;

    public MyProfileResponse getMyProfile(Long memberId) {
        return toProfile(getMember(memberId));
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long memberId, UpdateProfileRequest request) {
        Member member = getMember(memberId);
        member.updateProfile(request.name(), request.phone());
        return toProfile(member);
    }

    /**
     * 새 사진을 올리고 이전 사진은 커밋된 뒤 저장소에서 지운다.
     * 업로드 뒤 오류로 롤백되면 새로 올린 파일을 FileUploadService 가 지운다.
     *
     * @throws BusinessException 파일이 없으면 FILE_REQUIRED, 5MB 를 넘으면 FILE_TOO_LARGE,
     *                           JPG · PNG · WEBP 가 아니면 UNSUPPORTED_FILE_TYPE
     */
    @Transactional
    public MyProfileResponse changeProfileImage(Long memberId, MultipartFile image) {
        Member member = getMember(memberId);
        StoredFile stored = fileUploadService.upload(UploadPolicy.PROFILE_IMAGE,
                PROFILE_IMAGE_DIRECTORY.formatted(memberId), image);
        deleteProfileImageFile(member.changeProfileImage(stored.path()));
        return toProfile(member);
    }

    /** 기본 이미지로 돌아간다. 사진이 없어도 성공한다. */
    @Transactional
    public MyProfileResponse deleteProfileImage(Long memberId) {
        Member member = getMember(memberId);
        deleteProfileImageFile(member.removeProfileImage());
        return toProfile(member);
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

    /**
     * 진행 중인 신청 · 예약이 있으면 막는다. (상대방이 연락할 수 없는 예약이 남지 않게)
     *
     * @throws BusinessException 비밀번호가 틀리면 PASSWORD_MISMATCH,
     *                           이용자 · 공간 제공자로 진행 중인 신청 · 예약이 있으면 WITHDRAWAL_BLOCKED_BY_APPLICATION
     */
    @Transactional
    public void withdraw(Long memberId, String password) {
        Member member = getMember(memberId);
        verifyPassword(member, password);
        if (applicationRepository.existsInProgressByParticipant(memberId, LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.WITHDRAWAL_BLOCKED_BY_APPLICATION);
        }
        deleteProfileImageFile(member.removeProfileImage());
        member.withdraw(LocalDateTime.now(clock));
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }

    private MyProfileResponse toProfile(Member member) {
        return MyProfileResponse.of(member, profileImageResolver.urlOf(member));
    }

    /** @param storagePath null 이면 지울 사진이 없다 */
    private void deleteProfileImageFile(String storagePath) {
        if (storagePath != null) {
            fileUploadService.deleteAfterCommit(UploadPolicy.PROFILE_IMAGE, storagePath);
        }
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
