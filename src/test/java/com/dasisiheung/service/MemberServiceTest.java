package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.member.ChangePasswordRequest;
import com.dasisiheung.dto.member.UpdateProfileRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import com.dasisiheung.support.FixedClockConfig;
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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private MemberService memberService;
    private Member member;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, refreshTokenRepository, applicationRepository,
                passwordEncoder, FixedClockConfig.fixedClock());
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
    @DisplayName("탈퇴하면 개인정보를 지우고 리프레시 토큰을 폐기한다")
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
    @DisplayName("탈퇴한 회원은 MEMBER_NOT_FOUND")
    void withdrawnMemberIsNotFound() {
        member.withdraw(FixedClockConfig.FIXED_LOCAL_NOW);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.getMyProfile(MEMBER_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
