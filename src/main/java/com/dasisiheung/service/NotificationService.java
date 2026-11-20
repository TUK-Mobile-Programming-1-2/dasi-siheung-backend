package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.notification.NotificationCountResponse;
import com.dasisiheung.dto.notification.NotificationResponse;
import com.dasisiheung.entity.notification.Notification;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.repository.NotificationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 알림함. 다른 서비스가 {@link #send} 로 남기면 커밋 뒤 푸시가 나간다. ({@link PushNotificationService})
 * 알림 설정(끔 · 방해금지)은 푸시에만 적용하고, 알림함에는 항상 남긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 알림함에 남기고, 호출한 트랜잭션이 커밋되면 푸시를 보낸다. (롤백되면 둘 다 없던 일이 된다) */
    @Transactional
    public void send(NotificationDraft draft) {
        Notification notification = notificationRepository.save(new Notification(draft.recipientId(), draft.type(),
                draft.applicationId(), draft.title(), draft.body(), LocalDateTime.now(clock)));
        eventPublisher.publishEvent(NotificationCreatedEvent.from(notification));
    }

    /** 내 알림 (최근 순). unreadOnly 면 안 읽은 알림만. */
    public PageResponse<NotificationResponse> getMyNotifications(Long memberId, boolean unreadOnly,
                                                                 PageQuery pageQuery) {
        Pageable pageable = pageQuery.toPageable(LATEST_FIRST);
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadAtIsNull(memberId, pageable)
                : notificationRepository.findByRecipientId(memberId, pageable);
        return PageResponse.from(notifications, NotificationResponse::from);
    }

    public NotificationCountResponse countUnread(Long memberId) {
        return new NotificationCountResponse(notificationRepository.countByRecipientIdAndReadAtIsNull(memberId));
    }

    /**
     * 알림 하나를 읽음으로 바꾼다. 이미 읽었으면 그대로 돌려준다.
     *
     * @throws BusinessException 없거나 남의 알림이면 NOTIFICATION_NOT_FOUND
     */
    @Transactional
    public NotificationResponse markRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.getNotification(notificationId);
        notification.markRead(memberId, LocalDateTime.now(clock));
        return NotificationResponse.from(notification);
    }

    /** @return 이번에 읽음으로 바꾼 수 */
    @Transactional
    public NotificationCountResponse markAllRead(Long memberId) {
        return new NotificationCountResponse(notificationRepository.markAllRead(memberId, LocalDateTime.now(clock)));
    }
}
