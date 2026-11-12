package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.member.ChangePasswordRequest;
import com.dasisiheung.dto.member.UpdateProfileRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.NotificationSettingRepository;
import com.dasisiheung.repository.PushTokenRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.TestFiles;
import com.dasisiheung.util.FileType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final long MEMBER_ID = 7L;
    private static final String PASSWORD = "password123";

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private ProfileImageResolver profileImageResolver;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private MemberService memberService;
    private Member member;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, refreshTokenRepository, applicationRepository,
                notificationSettingRepository, pushTokenRepository, passwordEncoder, fileUploadService, profileImageResolver,
                FixedClockConfig.fixedClock());
        member = Member.signUp("user@example.com", passwordEncoder.encode(PASSWORD), "김시흥", "01012345678", Role.USER);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
    }

    @Test
    @DisplayName("이름만 보내면 이름만 바뀐다")
    void updateNameOnly() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        var response = memberService.updateMyProfile(MEMBER_ID, new UpdateProfileRequest("새이름", null));

        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.phone()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 모든 리프레시 토큰을 폐기한다")
    void changePassword() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        memberService.changePassword(MEMBER_ID, new ChangePasswordRequest(PASSWORD, "newPassword456"));

        assertThat(passwordEncoder.matches("newPassword456", member.getPasswordHash())).isTrue();
        then(refreshTokenRepository).should().deleteAllByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 PASSWORD_MISMATCH")
    void changePasswordWithWrongCurrent() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.changePassword(MEMBER_ID,
                new ChangePasswordRequest("wrong1234", "newPassword456")))
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        then(refreshTokenRepository).should(never()).deleteAllByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("새 비밀번호가 현재와 같으면 SAME_AS_CURRENT_PASSWORD")
    void changePasswordToSame() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.changePassword(MEMBER_ID, new ChangePasswordRequest(PASSWORD, PASSWORD)))
                .extracting("errorCode").isEqualTo(ErrorCode.SAME_AS_CURRENT_PASSWORD);
    }

    @Test
    @DisplayName("탈퇴하면 개인정보를 지우고 리프레시 토큰 · 알림 설정 · 푸시 토큰을 지운다")
    void withdraw() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        memberService.withdraw(MEMBER_ID, PASSWORD);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getEmail()).isEqualTo("withdrawn-7@deleted.invalid");
        assertThat(member.getName()).isEqualTo("탈퇴한 회원");
        assertThat(member.getPhone()).isNull();
        assertThat(member.getWithdrawnAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        assertThat(passwordEncoder.matches(PASSWORD, member.getPasswordHash())).isFalse();
        then(refreshTokenRepository).should().deleteAllByMemberId(MEMBER_ID);
        then(notificationSettingRepository).should().deleteById(MEMBER_ID);
        then(pushTokenRepository).should().deleteAllByMemberId(MEMBER_ID);
        then(applicationRepository).should().existsInProgressByParticipant(MEMBER_ID, FixedClockConfig.FIXED_TODAY);
    }

    @Test
    @DisplayName("진행 중인 신청 · 예약이 있으면 탈퇴를 막고 개인정보 · 토큰을 그대로 둔다")
    void withdrawBlockedByApplication() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(applicationRepository.existsInProgressByParticipant(MEMBER_ID, FixedClockConfig.FIXED_TODAY))
                .willReturn(true);

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, PASSWORD))
                .extracting("errorCode").isEqualTo(ErrorCode.WITHDRAWAL_BLOCKED_BY_APPLICATION);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getEmail()).isEqualTo("user@example.com");
        then(refreshTokenRepository).should(never()).deleteAllByMemberId(MEMBER_ID);
        then(notificationSettingRepository).should(never()).deleteById(MEMBER_ID);
        then(pushTokenRepository).should(never()).deleteAllByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 진행 중인 예약을 보기 전에 PASSWORD_MISMATCH")
    void withdrawChecksPasswordFirst() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, "wrong1234"))
                .extracting("errorCode").isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        then(applicationRepository).should(never()).existsInProgressByParticipant(anyLong(), any());
    }

    @Test
    @DisplayName("프로필 사진을 바꾸면 새 경로를 저장하고 이전 사진은 커밋 뒤 지운다")
    void changeProfileImage() {
        member.changeProfileImage("members/7/profile/old.jpg");
        var image = TestFiles.jpeg("image");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(fileUploadService.upload(UploadPolicy.PROFILE_IMAGE, "members/7/profile", image))
                .willReturn(new StoredFile("members/7/profile/new.jpg", FileType.JPEG, "photo.jpg", 10));
        given(profileImageResolver.urlOf(member)).willReturn("https://cdn/members/7/profile/new.jpg");

        var response = memberService.changeProfileImage(MEMBER_ID, image);

        assertThat(member.getProfileImagePath()).isEqualTo("members/7/profile/new.jpg");
        assertThat(response.profileImageUrl()).isEqualTo("https://cdn/members/7/profile/new.jpg");
        then(fileUploadService).should().deleteAfterCommit(UploadPolicy.PROFILE_IMAGE, "members/7/profile/old.jpg");
    }

    @Test
    @DisplayName("처음 올리는 프로필 사진이면 지울 파일이 없다")
    void changeProfileImageFirstTime() {
        var image = TestFiles.jpeg("image");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(fileUploadService.upload(eq(UploadPolicy.PROFILE_IMAGE), anyString(), eq(image)))
                .willReturn(new StoredFile("members/7/profile/new.jpg", FileType.JPEG, "photo.jpg", 10));

        memberService.changeProfileImage(MEMBER_ID, image);

        then(fileUploadService).should(never()).deleteAfterCommit(any(), anyString());
    }

    @Test
    @DisplayName("프로필 사진을 지우면 경로를 비우고 파일은 커밋 뒤 지운다. 사진이 없으면 아무것도 지우지 않는다")
    void deleteProfileImage() {
        member.changeProfileImage("members/7/profile/old.jpg");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        var response = memberService.deleteProfileImage(MEMBER_ID);
        memberService.deleteProfileImage(MEMBER_ID);

        assertThat(member.getProfileImagePath()).isNull();
        assertThat(response.profileImageUrl()).isNull();
        then(fileUploadService).should().deleteAfterCommit(UploadPolicy.PROFILE_IMAGE, "members/7/profile/old.jpg");
    }

    @Test
    @DisplayName("탈퇴하면 프로필 사진 파일도 커밋 뒤 지운다")
    void withdrawDeletesProfileImage() {
        member.changeProfileImage("members/7/profile/old.jpg");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        memberService.withdraw(MEMBER_ID, PASSWORD);

        assertThat(member.getProfileImagePath()).isNull();
        then(fileUploadService).should().deleteAfterCommit(UploadPolicy.PROFILE_IMAGE, "members/7/profile/old.jpg");
    }

    @Test
    @DisplayName("진행 중인 예약으로 탈퇴가 막히면 프로필 사진은 그대로 둔다")
    void withdrawBlockedKeepsProfileImage() {
        member.changeProfileImage("members/7/profile/old.jpg");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(applicationRepository.existsInProgressByParticipant(MEMBER_ID, FixedClockConfig.FIXED_TODAY))
                .willReturn(true);

        assertThatThrownBy(() -> memberService.withdraw(MEMBER_ID, PASSWORD))
                .extracting("errorCode").isEqualTo(ErrorCode.WITHDRAWAL_BLOCKED_BY_APPLICATION);
        assertThat(member.getProfileImagePath()).isEqualTo("members/7/profile/old.jpg");
        then(fileUploadService).should(never()).deleteAfterCommit(any(), anyString());
    }

    @Test
    @DisplayName("탈퇴한 회원은 MEMBER_NOT_FOUND")
    void withdrawnMemberIsNotFound() {
        member.withdraw(FixedClockConfig.FIXED_LOCAL_NOW);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.getMyProfile(MEMBER_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
