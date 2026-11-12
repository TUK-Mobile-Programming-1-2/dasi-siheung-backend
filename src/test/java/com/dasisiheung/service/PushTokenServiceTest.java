package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.PushToken;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.PushTokenRepository;
import com.dasisiheung.support.FixedClockConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    private static final long MEMBER_ID = 7L;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    private PushTokenService pushTokenService;
    private Member member;

    @BeforeEach
    void setUp() {
        pushTokenService = new PushTokenService(pushTokenRepository, memberRepository, FixedClockConfig.fixedClock());
        member = Member.signUp("user@example.com", "hash", "김시흥", null, Role.USER);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
    }

    @Test
    @DisplayName("처음 보는 토큰이면 새로 저장한다")
    void registerNew() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(pushTokenRepository.findByToken("token-1")).willReturn(Optional.empty());

        pushTokenService.register(MEMBER_ID, "token-1");

        ArgumentCaptor<PushToken> saved = ArgumentCaptor.forClass(PushToken.class);
        then(pushTokenRepository).should().save(saved.capture());
        assertThat(saved.getValue().getToken()).isEqualTo("token-1");
        assertThat(saved.getValue().getMember()).isSameAs(member);
        assertThat(saved.getValue().getLastRegisteredAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
    }

    @Test
    @DisplayName("이미 있는 토큰이면 이 회원에게 옮기고 등록 시각만 바꾼다 (새로 저장하지 않음)")
    void registerExisting() {
        Member previousOwner = Member.signUp("old@example.com", "hash", "이전", null, Role.PROVIDER);
        PushToken existing = PushToken.register(previousOwner, "token-1", FixedClockConfig.FIXED_LOCAL_NOW.minusDays(5));
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(pushTokenRepository.findByToken("token-1")).willReturn(Optional.of(existing));

        pushTokenService.register(MEMBER_ID, "token-1");

        assertThat(existing.getMember()).isSameAs(member);
        assertThat(existing.getLastRegisteredAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        then(pushTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("탈퇴한 회원은 MEMBER_NOT_FOUND")
    void registerWithdrawn() {
        member.withdraw(FixedClockConfig.FIXED_LOCAL_NOW);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> pushTokenService.register(MEMBER_ID, "token-1"))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        then(pushTokenRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("해제는 내 토큰만 지운다")
    void unregister() {
        pushTokenService.unregister(MEMBER_ID, "token-1");

        then(pushTokenRepository).should().deleteByMemberIdAndToken(MEMBER_ID, "token-1");
    }

    @Test
    @DisplayName("쓸 수 없는 토큰이 없으면 DB 를 건드리지 않는다")
    void deleteInvalid() {
        pushTokenService.deleteInvalid(List.of());
        then(pushTokenRepository).should(never()).deleteAllByTokenIn(anyCollection());

        pushTokenService.deleteInvalid(List.of("dead"));
        then(pushTokenRepository).should().deleteAllByTokenIn(List.of("dead"));
    }

    @Test
    @DisplayName("60일 넘게 다시 등록되지 않은 토큰을 지운다")
    void deleteStale() {
        given(pushTokenRepository.deleteAllRegisteredBefore(FixedClockConfig.FIXED_LOCAL_NOW.minusDays(60)))
                .willReturn(3);

        assertThat(pushTokenService.deleteStale()).isEqualTo(3);
    }
}
