package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.entity.member.DoNotDisturb;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.dasisiheung.entity.member.NotificationSetting;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.push.PushMessage;
import com.dasisiheung.push.PushResult;
import com.dasisiheung.push.PushSender;
import com.dasisiheung.repository.NotificationSettingRepository;
import com.dasisiheung.repository.PushTokenRepository;
import com.dasisiheung.support.FixedClockConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    private static final long RECIPIENT_ID = 2L;
    /** 21:00 KST — 기본 방해금지(22 → 8) 밖 */
    private static final Clock EVENING = FixedClockConfig.fixedClock();
    /** 23:30 KST — 기본 방해금지 안 */
    private static final Clock NIGHT = Clock.fixed(Instant.parse("2026-10-20T14:30:00Z"), FixedClockConfig.fixedClock().getZone());
    private static final long PROVIDER_ID = 3L;
    private static final ApplicationSubmittedEvent APPLICATION = new ApplicationSubmittedEvent(31L, 8L,
            "배곧 카페거리 2층 갤러리", PROVIDER_ID, "최팝업", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22),
            false);
    private static final ChatMessageSentEvent EVENT =
            new ChatMessageSentEvent(5L, 40L, 1L, "김시흥", RECIPIENT_ID, "  안녕하세요\n11월 첫 주말 가능할까요?  ");

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private PushTokenService pushTokenService;

    @Mock
    private PushSender pushSender;

    @Test
    @DisplayName("설정을 저장한 적 없으면 기본값으로 보고, 대화 상대의 모든 기기에 보낸다")
    void sendWithDefaults() {
        given(notificationSettingRepository.findById(RECIPIENT_ID)).willReturn(Optional.empty());
        given(pushTokenRepository.findTokensByMemberId(RECIPIENT_ID)).willReturn(List.of("phone", "tablet"));
        given(pushSender.send(anyList(), any())).willReturn(new PushResult(2, List.of()));

        service(EVENING).onChatMessageSent(EVENT);

        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        then(pushSender).should().send(eq(List.of("phone", "tablet")), message.capture());
        assertThat(message.getValue().title()).isEqualTo("김시흥");
        assertThat(message.getValue().body()).isEqualTo("안녕하세요 11월 첫 주말 가능할까요?");
        assertThat(message.getValue().data())
                .containsEntry("type", "CHAT_MESSAGE")
                .containsEntry("conversationId", "5")
                .containsEntry("messageId", "40");
        assertThat(message.getValue().channelId()).isEqualTo("chat");
        assertThat(message.getValue().tag()).isEqualTo("conversation-5");
        then(pushTokenService).should(never()).deleteInvalid(anyList());
    }

    @Test
    @DisplayName("기본 방해금지 시간(22 → 8)에는 보내지 않는다")
    void skipDuringDoNotDisturb() {
        given(notificationSettingRepository.findById(RECIPIENT_ID)).willReturn(Optional.empty());

        service(NIGHT).onChatMessageSent(EVENT);

        then(pushTokenRepository).should(never()).findTokensByMemberId(any());
        then(pushSender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("대화 알림을 끈 회원에게는 보내지 않는다")
    void skipWhenChatOff() {
        NotificationPreferences chatOff = new NotificationPreferences(true, false, true, false, false,
                DoNotDisturb.of(false, 22, 8));
        given(notificationSettingRepository.findById(RECIPIENT_ID)).willReturn(Optional.of(setting(chatOff)));

        service(EVENING).onChatMessageSent(EVENT);

        then(pushSender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("등록된 기기가 없으면 보내지 않는다")
    void skipWithoutTokens() {
        given(notificationSettingRepository.findById(RECIPIENT_ID)).willReturn(Optional.empty());
        given(pushTokenRepository.findTokensByMemberId(RECIPIENT_ID)).willReturn(List.of());

        service(EVENING).onChatMessageSent(EVENT);

        then(pushSender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("FCM 이 쓸 수 없다고 답한 토큰은 지운다")
    void deleteInvalidTokens() {
        given(notificationSettingRepository.findById(RECIPIENT_ID)).willReturn(Optional.empty());
        given(pushTokenRepository.findTokensByMemberId(RECIPIENT_ID)).willReturn(List.of("phone", "old-phone"));
        given(pushSender.send(anyList(), any())).willReturn(new PushResult(1, List.of("old-phone")));

        service(EVENING).onChatMessageSent(EVENT);

        then(pushTokenService).should().deleteInvalid(List.of("old-phone"));
    }

    @Test
    @DisplayName("새 이용 신청은 공간 제공자의 모든 기기에 보낸다 (활동 알림 채널)")
    void applicationSubmitted() {
        given(notificationSettingRepository.findById(PROVIDER_ID)).willReturn(Optional.empty());
        given(pushTokenRepository.findTokensByMemberId(PROVIDER_ID)).willReturn(List.of("provider-phone"));
        given(pushSender.send(anyList(), any())).willReturn(new PushResult(1, List.of()));

        service(EVENING).onApplicationSubmitted(APPLICATION);

        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        then(pushSender).should().send(eq(List.of("provider-phone")), message.capture());
        assertThat(message.getValue().title()).isEqualTo("새 이용 신청");
        assertThat(message.getValue().body()).isEqualTo("최팝업님이 '배곧 카페거리 2층 갤러리' 11/20~11/22 이용을 신청했어요.");
        assertThat(message.getValue().data())
                .containsEntry("type", "APPLICATION_SUBMITTED")
                .containsEntry("applicationId", "31")
                .containsEntry("spaceId", "8");
        assertThat(message.getValue().channelId()).isEqualTo("activity");
        assertThat(message.getValue().tag()).isEqualTo("application-31");
    }

    @Test
    @DisplayName("하루짜리 신청 · 가격 제안이 있으면 본문에 함께 보여준다")
    void applicationSubmittedOneDayWithOffer() {
        PushMessage message = PushNotificationService.applicationSubmitted(new ApplicationSubmittedEvent(31L, 8L,
                "배곧 카페거리 2층 갤러리", PROVIDER_ID, "최팝업", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20),
                true));

        assertThat(message.body()).isEqualTo("최팝업님이 '배곧 카페거리 2층 갤러리' 11/20 이용을 신청했어요. (희망 가격 제안 있음)");
    }

    @Test
    @DisplayName("활동 알림을 끈 공간 제공자에게는 신청 알림을 보내지 않는다 (대화 알림과 별개)")
    void skipApplicationWhenActivityOff() {
        NotificationPreferences activityOff = new NotificationPreferences(true, true, false, false, false,
                DoNotDisturb.of(false, 22, 8));
        given(notificationSettingRepository.findById(PROVIDER_ID)).willReturn(Optional.of(setting(activityOff)));

        service(EVENING).onApplicationSubmitted(APPLICATION);

        then(pushSender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("방해금지 시간에는 신청 알림도 보내지 않는다")
    void skipApplicationDuringDoNotDisturb() {
        given(notificationSettingRepository.findById(PROVIDER_ID)).willReturn(Optional.empty());

        service(NIGHT).onApplicationSubmitted(APPLICATION);

        then(pushSender).should(never()).send(anyList(), any());
    }

    @Test
    @DisplayName("본문은 100자까지 자르고 말줄임표를 붙인다")
    void longMessagePreview() {
        String content = "가".repeat(150);

        PushMessage message = PushNotificationService.chatMessage(
                new ChatMessageSentEvent(5L, 40L, 1L, "김시흥", RECIPIENT_ID, content));

        assertThat(message.body()).hasSize(101).endsWith("…");
    }

    private PushNotificationService service(Clock clock) {
        return new PushNotificationService(notificationSettingRepository, pushTokenRepository, pushTokenService,
                pushSender, clock);
    }

    private static NotificationSetting setting(NotificationPreferences preferences) {
        Member member = Member.signUp("user@example.com", "hash", "이배곧", null, Role.PROVIDER);
        return NotificationSetting.create(member, preferences, FixedClockConfig.FIXED_LOCAL_NOW);
    }
}
