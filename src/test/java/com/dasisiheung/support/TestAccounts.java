package com.dasisiheung.support;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.security.JwtTokenProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 통합 테스트용 회원을 만들고 실제 JWT 를 발급한다. 관리자는 DBA 승격처럼 역할만 바꿔 저장한다.
 */
@TestComponent
@RequiredArgsConstructor
public class TestAccounts {

    public static final String PASSWORD = "password123";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Account create(Role role) {
        String email = "it-" + UUID.randomUUID() + "@test.dasisiheung.com";
        Role signUpRole = role == Role.ADMIN ? Role.USER : role;
        Member member = Member.signUp(email, passwordEncoder.encode(PASSWORD), "통합테스트", "01012345678", signUpRole);
        if (role == Role.ADMIN) {
            ReflectionTestUtils.setField(member, "role", Role.ADMIN);
        }
        memberRepository.saveAndFlush(member);
        return new Account(member.getId(), email, role,
                "Bearer " + jwtTokenProvider.createAccessToken(member.getId(), role));
    }

    public record Account(Long id, String email, Role role, String bearer) {
    }
}
