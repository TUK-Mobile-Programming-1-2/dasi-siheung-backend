package com.dasisiheung.service;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.PushToken;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PushTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 푸시 알림 기기 토큰 등록 · 해제 · 정리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    /** 이 기간 동안 다시 등록되지 않은 토큰은 쓰지 않는 기기로 본다. (앱은 시작할 때마다 등록한다) */
    static final Duration STALE_AFTER = Duration.ofDays(60);

    private final PushTokenRepository pushTokenRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    /**
     * 앱 시작 · 로그인 · 토큰 갱신 때 호출한다. 같은 토큰을 여러 번 보내도 되고,
     * 다른 계정이 쓰던 토큰이면 이 회원에게 옮긴다. (한 기기 = 마지막으로 로그인한 계정만 알림을 받는다)
     *
     * @throws BusinessException 없거나 탈퇴한 회원이면 MEMBER_NOT_FOUND
     */
    @Transactional
    public void register(Long memberId, String token) {
        Member member = memberRepository.findById(memberId)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        pushTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> existing.renew(member, now),
                () -> pushTokenRepository.save(PushToken.register(member, token, now)));
    }

    /** 로그아웃 전에 호출한다. 내 토큰이 아니거나 이미 없어도 성공으로 본다. */
    @Transactional
    public void unregister(Long memberId, String token) {
        pushTokenRepository.deleteByMemberIdAndToken(memberId, token);
    }

    /** FCM 이 더 이상 쓸 수 없다고 답한 토큰. */
    @Transactional
    public void deleteInvalid(Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            pushTokenRepository.deleteAllByTokenIn(tokens);
        }
    }

    /** @return 지운 수 */
    @Transactional
    public int deleteStale() {
        return pushTokenRepository.deleteAllRegisteredBefore(LocalDateTime.now(clock).minus(STALE_AFTER));
    }
}
