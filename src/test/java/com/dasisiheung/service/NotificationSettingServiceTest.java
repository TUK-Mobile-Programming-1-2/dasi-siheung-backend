package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.member.NotificationSettingRequest;
import com.dasisiheung.dto.member.NotificationSettingResponse;
import com.dasisiheung.entity.member.DoNotDisturb;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.dasisiheung.entity.member.NotificationSetting;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.NotificationSettingRepository;
import com.dasisiheung.support.FixedClockConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceTest {

    private static final long MEMBER_ID = 7L;
    private static final LocalDateTime EARLIER = FixedClockConfig.FIXED_LOCAL_NOW.minusDays(10);
    private static final NotificationSettingRequest REQUEST =
            new NotificationSettingRequest(true, false, true, true, true, true, 23, 7);

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private MemberRepository memberRepository;

    private NotificationSettingService notificationSettingService;
    private Member member;

    @BeforeEach
    void setUp() {
        notificationSettingService = new NotificationSettingService(notificationSettingRepository, memberRepository,
                FixedClockConfig.fixedClock());
        member = Member.signUp("user@example.com", "hash", "김시흥", "01012345678", Role.USER);
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
    }

    @Test
    @DisplayName("저장한 적이 없으면 기본값을 주고 행을 만들지 않는다")
    void getDefaults() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(notificationSettingRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        NotificationSettingResponse response = notificationSettingService.getMySettings(MEMBER_ID);

        assertThat(response).isEqualTo(NotificationSettingResponse.defaults());
        assertThat(response.updatedAt()).isNull();
        then(notificationSettingRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("저장한 값이 있으면 그 값을 준다")
    void getSaved() {
        NotificationSetting saved = NotificationSetting.create(member, REQUEST.toPreferences(), EARLIER);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(notificationSettingRepository.findById(MEMBER_ID)).willReturn(Optional.of(saved));

        NotificationSettingResponse response = notificationSettingService.getMySettings(MEMBER_ID);

        assertThat(response.chat()).isFalse();
        assertThat(response.dndStartHour()).isEqualTo(23);
        assertThat(response.dndEndHour()).isEqualTo(7);
        assertThat(response.marketingConsentedAt()).isEqualTo(EARLIER);
    }

    @Test
    @DisplayName("처음 저장하면 새로 만들고, 광고 동의 시각은 현재 시각이다")
    void createOnFirstReplace() {
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(notificationSettingRepository.findById(MEMBER_ID)).willReturn(Optional.empty());
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        NotificationSettingResponse response = notificationSettingService.replaceMySettings(MEMBER_ID, REQUEST);

        assertThat(response.chat()).isFalse();
        assertThat(response.favoriteSpace()).isTrue();
        assertThat(response.marketing()).isTrue();
        assertThat(response.marketingConsentedAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        then(notificationSettingRepository).should().save(any(NotificationSetting.class));
    }

    @Test
    @DisplayName("이미 있으면 모든 값을 바꾸고, 계속 동의 중이면 처음 동의 시각을 유지한다")
    void replaceExisting() {
        NotificationSetting saved = NotificationSetting.create(member, new NotificationPreferences(
                false, true, false, false, true, DoNotDisturb.of(false, 22, 8)), EARLIER);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(notificationSettingRepository.findById(MEMBER_ID)).willReturn(Optional.of(saved));

        NotificationSettingResponse response = notificationSettingService.replaceMySettings(MEMBER_ID, REQUEST);

        assertThat(saved.preferences()).isEqualTo(REQUEST.toPreferences());
        assertThat(response.push()).isTrue();
        assertThat(response.dndEnabled()).isTrue();
        assertThat(response.marketingConsentedAt()).isEqualTo(EARLIER);
        then(notificationSettingRepository).should(never()).save(any());
        then(notificationSettingRepository).should().flush();
    }

    @Test
    @DisplayName("탈퇴한 회원은 MEMBER_NOT_FOUND")
    void withdrawnMember() {
        member.withdraw(FixedClockConfig.FIXED_LOCAL_NOW);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> notificationSettingService.getMySettings(MEMBER_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThatThrownBy(() -> notificationSettingService.replaceMySettings(MEMBER_ID, REQUEST))
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        then(notificationSettingRepository).should(never()).save(any());
    }
}
