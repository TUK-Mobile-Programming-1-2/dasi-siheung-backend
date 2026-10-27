package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.auth.LoginRequest;
import com.dasisiheung.dto.auth.SignupRequest;
import com.dasisiheung.dto.auth.TokenResponse;
import com.dasisiheung.entity.auth.RefreshToken;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import com.dasisiheung.security.JwtProperties;
import com.dasisiheung.security.JwtTokenProvider;
import com.dasisiheung.security.RefreshTokenGenerator;
import com.dasisiheung.support.FixedClockConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "password123";
    private static final LocalDateTime NOW = FixedClockConfig.FIXED_LOCAL_NOW;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final RefreshTokenGenerator refreshTokenGenerator = new RefreshTokenGenerator();
    private final Clock clock = FixedClockConfig.fixedClock();
    private final JwtProperties jwtProperties = new JwtProperties(
            "test-only-jwt-secret-key-for-dasi-siheung-0123456789", Duration.ofMinutes(30), Duration.ofDays(14));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(memberRepository, refreshTokenRepository, passwordEncoder,
                new JwtTokenProvider(jwtProperties, clock), refreshTokenGenerator, jwtProperties, clock);
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("비밀번호를 암호화해 저장하고 정규화된 이메일을 돌려준다")
        void success() {
            given(memberRepository.existsByEmail("user@example.com")).willReturn(false);
            given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));

            var response = authService.signup(signupRequest("User@Example.com", Role.USER));

            ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
            then(memberRepository).should().saveAndFlush(saved.capture());
            assertThat(passwordEncoder.matches(RAW_PASSWORD, saved.getValue().getPasswordHash())).isTrue();
            assertThat(response.email()).isEqualTo("user@example.com");
            assertThat(response.role()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL")
        void duplicateEmail() {
            given(memberRepository.existsByEmail("user@example.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(signupRequest("user@example.com", Role.USER)))
                    .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
            then(memberRepository).should(never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("동시 가입으로 UNIQUE 제약에 걸려도 DUPLICATE_EMAIL")
        void duplicateEmailRace() {
            given(memberRepository.existsByEmail("user@example.com")).willReturn(false);
            given(memberRepository.saveAndFlush(any(Member.class))).willThrow(new DataIntegrityViolationException("uk_member_email"));

            assertThatThrownBy(() -> authService.signup(signupRequest("user@example.com", Role.USER)))
                    .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
        }

        @Test
        @DisplayName("관리자 역할은 ROLE_NOT_ALLOWED")
        void adminNotAllowed() {
            given(memberRepository.existsByEmail("admin@example.com")).willReturn(false);

            assertThatThrownBy(() -> authService.signup(signupRequest("admin@example.com", Role.ADMIN)))
                    .extracting("errorCode").isEqualTo(ErrorCode.ROLE_NOT_ALLOWED);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공하면 토큰 쌍을 발급하고 리프레시 토큰은 해시로 저장한다")
        void success() {
            Member member = savedMember(1L, Role.PROVIDER);
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(member));

            TokenResponse response = authService.login(new LoginRequest("USER@example.com", RAW_PASSWORD));

            ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
            then(refreshTokenRepository).should().save(saved.capture());
            assertThat(saved.getValue().getTokenHash()).isEqualTo(refreshTokenGenerator.hash(response.refreshToken()));
            assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusDays(14));
            assertThat(response.accessTokenExpiresIn()).isEqualTo(1800);
            assertThat(response.member().role()).isEqualTo(Role.PROVIDER);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS")
        void wrongPassword() {
            given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(savedMember(1L, Role.USER)));

            assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong-password1")))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("없는 이메일도 같은 INVALID_CREDENTIALS (계정 존재 여부를 알려주지 않음)")
        void unknownEmail() {
            given(memberRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", RAW_PASSWORD)))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {

        @Test
        @DisplayName("사용한 토큰을 지우고 새 토큰을 발급한다")
        void rotates() {
            String raw = refreshTokenGenerator.generate();
            RefreshToken stored = RefreshToken.issue(savedMember(1L, Role.USER), refreshTokenGenerator.hash(raw),
                    NOW.minusDays(1), NOW.plusDays(13));
            given(refreshTokenRepository.findWithMemberByTokenHash(refreshTokenGenerator.hash(raw)))
                    .willReturn(Optional.of(stored));

            TokenResponse response = authService.refresh(raw);

            then(refreshTokenRepository).should().delete(stored);
            assertThat(response.refreshToken()).isNotEqualTo(raw);
        }

        @Test
        @DisplayName("만료된 토큰이면 INVALID_REFRESH_TOKEN")
        void expired() {
            String raw = refreshTokenGenerator.generate();
            RefreshToken stored = RefreshToken.issue(savedMember(1L, Role.USER), refreshTokenGenerator.hash(raw),
                    NOW.minusDays(14), NOW);
            given(refreshTokenRepository.findWithMemberByTokenHash(any())).willReturn(Optional.of(stored));

            assertThatThrownBy(() -> authService.refresh(raw))
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("저장되지 않은(폐기된) 토큰이면 INVALID_REFRESH_TOKEN")
        void unknown() {
            given(refreshTokenRepository.findWithMemberByTokenHash(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("revoked"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Test
    @DisplayName("로그아웃은 토큰 해시로 삭제한다")
    void logout() {
        authService.logout("raw-token");

        then(refreshTokenRepository).should().deleteByTokenHash(refreshTokenGenerator.hash("raw-token"));
    }

    private Member savedMember(Long id, Role role) {
        Member member = Member.signUp("user@example.com", passwordEncoder.encode(RAW_PASSWORD), "김시흥", "01012345678", role);
        return withId(member, id);
    }

    private static Member withId(Member member, Long id) {
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private static SignupRequest signupRequest(String email, Role role) {
        return new SignupRequest(email, RAW_PASSWORD, "김시흥", "010-1234-5678", role);
    }
}
