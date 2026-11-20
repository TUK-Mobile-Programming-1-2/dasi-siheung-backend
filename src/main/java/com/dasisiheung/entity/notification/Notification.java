package com.dasisiheung.entity.notification;

import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 알림함의 항목 하나. 푸시 수신 여부(알림 설정 · 방해금지 · 기기 토큰)와 상관없이 항상 남는다.
 * 제목 · 본문은 만든 시점의 문구를 그대로 보관한다.
 */
@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    public static final int MAX_TITLE_LENGTH = 50;
    public static final int MAX_BODY_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private NotificationType type;

    @Column(name = "application_id", updatable = false)
    private Long applicationId;

    @Column(nullable = false, updatable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, updatable = false, length = MAX_BODY_LENGTH)
    private String body;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * @param recipientId   받는 회원
     * @param applicationId 눌렀을 때 열 신청 (신청과 관계없는 알림이면 null)
     */
    public Notification(Long recipientId, NotificationType type, Long applicationId, String title, String body,
                        LocalDateTime createdAt) {
        this.recipientId = recipientId;
        this.type = type;
        this.applicationId = applicationId;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    /**
     * 읽음으로 표시한다. 이미 읽었으면 처음 읽은 시각을 그대로 둔다.
     *
     * @throws BusinessException 내 알림이 아니면 NOTIFICATION_NOT_FOUND (남의 알림이 있는지 드러내지 않는다)
     */
    public void markRead(Long memberId, LocalDateTime now) {
        if (!recipientId.equals(memberId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if (readAt == null) {
            this.readAt = now;
        }
    }
}
