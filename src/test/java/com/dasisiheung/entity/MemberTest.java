package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class MemberTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 21, 0);

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"USER", "PROVIDER"})
    @DisplayName("이용자·제공자는 가입할 수 있고, 이메일은 소문자·전화번호는 숫자만 저장된다")
    void signUp(Role role) {
        Member member = Member.signUp("  User@Example.COM ", "hash", " 김시흥 ", "010-1234-5678", role);

        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getName()).isEqualTo("김시흥");
        assertThat(member.getPhone()).isEqualTo("01012345678");
        assertThat(member.getRole()).isEqualTo(role);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자 역할로는 가입할 수 없다")
    void adminCannotSignUp() {
        assertThatThrownBy(() -> Member.signUp("admin@example.com", "hash", "관리자", "01012345678", Role.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("정지 기간 중에는 로그인할 수 없고, 메시지에 해제 예정일이 들어간다")
    void suspendedMemberCannotSignIn() {
        Member member = suspended(NOW.plusDays(3));

        assertThatThrownBy(() -> member.validateCanSignIn(NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2026-10-23")
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    @DisplayName("정지 기간이 지나면 로그인 시 자동으로 정상 상태가 된다")
    void suspensionExpires() {
        Member member = suspended(NOW.minusMinutes(1));

        member.validateCanSignIn(NOW);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getSuspendedUntil()).isNull();
    }

    @Test
    @DisplayName("탈퇴 회원은 존재하지 않는 계정처럼 INVALID_CREDENTIALS 로 거부한다")
    void withdrawnMemberCannotSignIn() {
        Member member = Member.signUp("user@example.com", "hash", "김시흥", "01012345678", Role.USER);
        ReflectionTestUtils.setField(member, "status", MemberStatus.WITHDRAWN);

        assertThatThrownBy(() -> member.validateCanSignIn(NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("프로필 사진을 바꾸거나 지우면 이전 경로를 돌려준다 (저장소에서 지울 대상)")
    void changeAndRemoveProfileImage() {
        Member member = Member.signUp("user@example.com", "hash", "김시흥", "01012345678", Role.USER);

        assertThat(member.changeProfileImage("members/1/profile/a.jpg")).isNull();
        assertThat(member.changeProfileImage("members/1/profile/b.jpg")).isEqualTo("members/1/profile/a.jpg");
        assertThat(member.removeProfileImage()).isEqualTo("members/1/profile/b.jpg");
        assertThat(member.getProfileImagePath()).isNull();
        assertThat(member.removeProfileImage()).isNull();
    }

    @Test
    @DisplayName("탈퇴하면 프로필 사진 경로도 지운다")
    void withdrawClearsProfileImage() {
        Member member = Member.signUp("user@example.com", "hash", "김시흥", "01012345678", Role.USER);
        member.changeProfileImage("members/1/profile/a.jpg");

        member.withdraw(NOW);

        assertThat(member.getProfileImagePath()).isNull();
    }

    private static Member suspended(LocalDateTime until) {
        Member member = Member.signUp("user@example.com", "hash", "김시흥", "01012345678", Role.USER);
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        ReflectionTestUtils.setField(member, "suspendedUntil", until);
        return member;
    }
}
