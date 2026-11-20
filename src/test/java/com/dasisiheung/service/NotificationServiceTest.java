package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.notification.NotificationResponse;
import com.dasisiheung.entity.notification.Notification;
import com.dasisiheung.entity.notification.NotificationType;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.NotificationRepository;
import com.dasisiheung.support.FixedClockConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long MEMBER_ID = 5L;
    private static final LocalDateTime NOW = FixedClockConfig.FIXED_LOCAL_NOW;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, eventPublisher, FixedClockConfig.fixedClock());
    }

    @Test
    @DisplayName("알림함에 남기고, 저장된 ID 로 푸시 이벤트를 낸다 (커밋 뒤 전송)")
    void send() {
        given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });

        notificationService.send(new NotificationDraft(MEMBER_ID, NotificationType.PRICE_OFFER_RECEIVED, 31L,
                "공간 제공자의 역제안", "제공자님이 95,000원을 제안했어요."));

        ArgumentCaptor<NotificationCreatedEvent> event = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new NotificationCreatedEvent(12L, MEMBER_ID,
                NotificationType.PRICE_OFFER_RECEIVED, 31L, "공간 제공자의 역제안", "제공자님이 95,000원을 제안했어요.", NOW));
    }

    @Test
    @DisplayName("목록은 최근 순, unreadOnly 면 안 읽은 알림만 조회한다")
    void getMyNotifications() {
        Notification unread = notification(12L);
        given(notificationRepository.findByRecipientIdAndReadAtIsNull(any(), any()))
                .willReturn(new PageImpl<>(List.of(unread)));

        PageResponse<NotificationResponse> page = notificationService.getMyNotifications(MEMBER_ID, true,
                new PageQuery(0, 20));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(notificationRepository).should().findByRecipientIdAndReadAtIsNull(eq(MEMBER_ID), pageable.capture());
        then(notificationRepository).should(never()).findByRecipientId(any(), any());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        assertThat(page.content()).extracting(NotificationResponse::id).containsExactly(12L);
        assertThat(page.content().get(0).read()).isFalse();
        assertThat(page.content().get(0).applicationId()).isEqualTo(31L);
    }

    @Test
    @DisplayName("전체 목록")
    void getAllNotifications() {
        Page<Notification> notifications = new PageImpl<>(List.of(notification(13L), notification(12L)));
        given(notificationRepository.findByRecipientId(eq(MEMBER_ID), any()))
                .willReturn(notifications);

        PageResponse<NotificationResponse> page = notificationService.getMyNotifications(MEMBER_ID, false,
                new PageQuery(null, null));

        assertThat(page.content()).extracting(NotificationResponse::id).containsExactly(13L, 12L);
    }

    @Test
    @DisplayName("읽음 처리: 내 알림이면 읽은 시각을 남겨 돌려준다")
    void markRead() {
        given(notificationRepository.getNotification(12L)).willReturn(notification(12L));

        NotificationResponse response = notificationService.markRead(MEMBER_ID, 12L);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("읽음 처리: 남의 알림이면 NOTIFICATION_NOT_FOUND")
    void markOthersRead() {
        given(notificationRepository.getNotification(12L)).willReturn(notification(12L));

        assertThatThrownBy(() -> notificationService.markRead(99L, 12L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("안 읽은 수 · 모두 읽음")
    void counts() {
        given(notificationRepository.countByRecipientIdAndReadAtIsNull(MEMBER_ID)).willReturn(3L);
        given(notificationRepository.markAllRead(MEMBER_ID, NOW)).willReturn(3);

        assertThat(notificationService.countUnread(MEMBER_ID).count()).isEqualTo(3L);
        assertThat(notificationService.markAllRead(MEMBER_ID).count()).isEqualTo(3L);
    }

    private static Notification notification(long id) {
        Notification notification = new Notification(MEMBER_ID, NotificationType.PRICE_OFFER_RECEIVED, 31L,
                "공간 제공자의 역제안", "제공자님이 95,000원을 제안했어요.", NOW);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
