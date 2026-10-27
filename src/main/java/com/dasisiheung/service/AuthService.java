package com.dasisiheung.service;

import com.dasisiheung.dto.auth.LoginRequest;
import com.dasisiheung.dto.auth.SignupRequest;
import com.dasisiheung.dto.auth.TokenResponse;
import com.dasisiheung.dto.member.MemberSummaryResponse;
import com.dasisiheung.entity.auth.RefreshToken;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.RefreshTokenRepository;
import com.dasisiheung.security.JwtProperties;
import com.dasisiheung.security.JwtTokenProvider;
import com.dasisiheung.security.RefreshTokenGenerator;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 · 로그인 · 토큰 재발급 · 로그아웃.
 * <p>
 * 액세스 토큰(JWT)은 서버에 저장하지 않고, 리프레시 토큰은 해시로 저장해 재발급할 때마다 교체한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public MemberSummaryResponse signup(SignupRequest request) {
        String email = Member.normalizeEmail(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        Member member = Member.signUp(email, passwordEncoder.encode(request.password()),
                request.name(), request.phone(), request.role());
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            // 같은 이메일로 동시에 가입한 경우 (email UNIQUE 제약)
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        return MemberSummaryResponse.from(member);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(Member.normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        member.validateCanSignIn(now());
        return issueTokens(member);
    }

    /**
     * 리프레시 토큰으로 새 토큰 쌍을 발급한다. 사용한 리프레시 토큰은 즉시 폐기된다.
     */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findWithMemberByTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .filter(found -> !found.isExpired(now()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        Member member = refreshToken.getMember();
        member.validateCanSignIn(now());

        refreshTokenRepository.delete(refreshToken);
        return issueTokens(member);
    }

    /** 이미 폐기된 토큰이어도 성공으로 처리한다. (멱등) */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.deleteByTokenHash(refreshTokenGenerator.hash(rawRefreshToken));
    }

    @Transactional
    public int deleteExpiredRefreshTokens() {
        return refreshTokenRepository.deleteAllExpired(now());
    }

    private TokenResponse issueTokens(Member member) {
        LocalDateTime issuedAt = now();
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String rawRefreshToken = refreshTokenGenerator.generate();
        refreshTokenRepository.save(RefreshToken.issue(member, refreshTokenGenerator.hash(rawRefreshToken),
                issuedAt, issuedAt.plus(jwtProperties.refreshTokenValidity())));

        return TokenResponse.of(accessToken, jwtProperties.accessTokenValidity(),
                rawRefreshToken, jwtProperties.refreshTokenValidity(), MemberSummaryResponse.from(member));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
