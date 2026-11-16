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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
 * (앱은 폴링 · 목록 조회로 새 메시지 · 신청을 그대로 볼 수 있다)
 * <ul>
 *   <li>채팅 메시지 → 대화 상대 (대화 알림). data-only 로 보내 앱이 메시지를 쌓고 읽음 · 답장 버튼을 단다.</li>
 *   <li>새 이용 신청 → 공간 제공자 (활동 알림). 시스템이 바로 그린다.</li>
 *   <li>1:1 문의 답변 → 문의한 회원 (활동 알림). 시스템이 바로 그린다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    static final String ACTIVITY_CHANNEL = "activity";
    static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";
    static final String TYPE_APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    static final String TYPE_INQUIRY_ANSWERED = "INQUIRY_ANSWERED";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d");
    /** 푸시 data 의 시각 형식 — API 응답의 LocalDateTime 과 같다 (예: 2026-11-13T11:30:00). */
    private static final DateTimeFormatter DATA_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    /** 채팅 푸시에 담는 내용 길이 (글자 수). 앱이 알림 크기에 맞게 다시 줄인다. */
    static final int MAX_CHAT_CONTENT_LENGTH = 500;

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

    /** 공간 제공자에게 새 이용 신청을 알린다. (알림 설정의 활동 알림) */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        send(event.providerId(), NotificationCategory.ACTIVITY, applicationSubmitted(event));
    }

    /** 문의한 회원에게 1:1 문의 답변을 알린다. (알림 설정의 활동 알림) */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInquiryAnswered(InquiryAnsweredEvent event) {
        send(event.memberId(), NotificationCategory.ACTIVITY, inquiryAnswered(event));
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

    /**
     * data-only: 앱이 onMessageReceived 로 받아 대화방별로 메시지를 쌓고 읽음 · 답장 버튼을 단다. (채널 · tag 도 앱이 정함)
     */
    static PushMessage chatMessage(ChatMessageSentEvent event) {
        return PushMessage.dataOnly(Map.of(
                "type", TYPE_CHAT_MESSAGE,
                "conversationId", String.valueOf(event.conversationId()),
                "messageId", String.valueOf(event.messageId()),
                "senderName", event.senderName(),
                "content", truncate(event.content(), MAX_CHAT_CONTENT_LENGTH),
                "createdAt", DATA_TIME.format(event.createdAt().truncatedTo(ChronoUnit.SECONDS))));
    }

    /** 제목: 새 이용 신청 / 본문: 누가 · 어느 공간 · 언제 (+ 가격 제안) / 신청마다 알림 하나. */
    static PushMessage applicationSubmitted(ApplicationSubmittedEvent event) {
        String body = "%s님이 '%s' %s 이용을 신청했어요.%s".formatted(event.applicantName(), event.spaceTitle(),
                period(event.startDate(), event.endDate()), event.withOffer() ? " (희망 가격 제안 있음)" : "");
        Map<String, String> data = Map.of(
                "type", TYPE_APPLICATION_SUBMITTED,
                "applicationId", String.valueOf(event.applicationId()),
                "spaceId", String.valueOf(event.spaceId()));
        return PushMessage.notification("새 이용 신청", body, data, ACTIVITY_CHANNEL,
                "application-" + event.applicationId());
    }

    /** 제목: 1:1 문의 답변 / 본문: 어느 분류의 문의인지 / 문의마다 알림 하나. 누르면 문의 상세로 간다. */
    static PushMessage inquiryAnswered(InquiryAnsweredEvent event) {
        String body = "'%s' 문의에 답변이 등록됐어요.".formatted(event.category().getLabel());
        Map<String, String> data = Map.of(
                "type", TYPE_INQUIRY_ANSWERED,
                "inquiryId", String.valueOf(event.inquiryId()));
        return PushMessage.notification("1:1 문의 답변", body, data, ACTIVITY_CHANNEL, "inquiry-" + event.inquiryId());
    }

    /** 11/20 또는 11/20~11/22 */
    private static String period(LocalDate start, LocalDate end) {
        return start.equals(end) ? SHORT_DATE.format(start) : SHORT_DATE.format(start) + "~" + SHORT_DATE.format(end);
    }

    /** 글자(코드 포인트) 단위로 잘라 이모지를 반으로 자르지 않는다. 잘랐으면 말줄임표를 붙인다. */
    private static String truncate(String content, int maxLength) {
        if (content.codePointCount(0, content.length()) <= maxLength) {
            return content;
        }
        return content.substring(0, content.offsetByCodePoints(0, maxLength)) + "…";
    }
}
