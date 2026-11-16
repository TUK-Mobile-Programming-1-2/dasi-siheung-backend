package com.dasisiheung.service;

import java.time.LocalDateTime;

/**
 * 1:1 대화에 새 메시지가 저장됐다. 커밋된 뒤 받는 사람에게 푸시를 보낸다. ({@link PushNotificationService})
 *
 * @param recipientId 받는 사람 (보낸 사람의 대화 상대)
 * @param content     저장된 메시지 내용
 * @param createdAt   보낸 시각
 */
public record ChatMessageSentEvent(
        Long conversationId,
        Long messageId,
        Long senderId,
        String senderName,
        Long recipientId,
        String content,
        LocalDateTime createdAt
) {
}
