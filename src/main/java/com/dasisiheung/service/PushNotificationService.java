package com.dasisiheung.service;

import com.dasisiheung.entity.member.NotificationCategory;
import com.dasisiheung.entity.member.NotificationPreferences;
import com.dasisiheung.entity.member.NotificationSetting;
import com.dasisiheung.push.PushMessage;
import com.dasisiheung.push.PushResult;
import com.dasisiheung.push.PushSender;
import com.dasisiheung.repository.NotificationSettingRepository;
import com.dasisiheung.repository.PushTokenRepository;
import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 저장이 커밋된 일에 대해 받는 사람에게 푸시를 보낸다. 응답을 늦추지 않도록 별도 스레드에서 실행한다.
 * <p>
 * 회원의 알림 설정(전체 스위치 · 분류 · 방해금지)을 지킨다. 방해금지 시간의 알림은 미루지 않고 버린다.
 * (앱은 폴링 · 안 읽은 수로 새 메시지를 그대로 볼 수 있다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    static final String CHAT_CHANNEL = "chat";
    static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";
    static final int MAX_BODY_LENGTH = 100;

    private final NotificationSettingRepository notificationSettingRepository;
    private final PushTokenRepository pushTokenRepository;
    private final PushTokenService pushTokenService;
    private final PushSender pushSender;
    private final Clock clock;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageSent(ChatMessageSentEvent event) {
        send(event.recipientId(), NotificationCategory.CHAT, chatMessage(event));
    }

    private void send(Long recipientId, NotificationCategory category, PushMessage message) {
        if (!preferencesOf(recipientId).allowsPush(category, LocalTime.now(clock))) {
            log.debug("회원 {} 의 알림 설정으로 {} 푸시를 보내지 않음", recipientId, category);
            return;
        }
        List<String> tokens = pushTokenRepository.findTokensByMemberId(recipientId);
        if (tokens.isEmpty()) {
            return;
        }
        PushResult result = pushSender.send(tokens, message);
        if (!result.invalidTokens().isEmpty()) {
            pushTokenService.deleteInvalid(result.invalidTokens());
            log.info("쓸 수 없는 푸시 토큰 {}개 정리 (회원 {})", result.invalidTokens().size(), recipientId);
        }
    }

    private NotificationPreferences preferencesOf(Long memberId) {
        return notificationSettingRepository.findById(memberId)
                .map(NotificationSetting::preferences)
                .orElse(NotificationPreferences.DEFAULT);
    }

    /** 제목: 보낸 사람 / 본문: 메시지 앞부분 / 대화방마다 알림 하나만 보이게 tag 를 붙인다. */
    static PushMessage chatMessage(ChatMessageSentEvent event) {
        Map<String, String> data = Map.of(
                "type", TYPE_CHAT_MESSAGE,
                "conversationId", String.valueOf(event.conversationId()),
                "messageId", String.valueOf(event.messageId()));
        return new PushMessage(event.senderName(), preview(event.content()), data, CHAT_CHANNEL,
                "conversation-" + event.conversationId());
    }

    private static String preview(String content) {
        String flat = content.strip().replaceAll("\\s+", " ");
        return flat.length() <= MAX_BODY_LENGTH ? flat : flat.substring(0, MAX_BODY_LENGTH) + "…";
    }
}
