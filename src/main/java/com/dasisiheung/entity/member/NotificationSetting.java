package com.dasisiheung.entity.member;

import com.dasisiheung.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 한 명의 알림 설정. 회원 ID 를 그대로 기본 키로 쓴다. (회원당 하나)
 * <p>
 * 광고성 정보 수신은 동의한 시각을 남긴다. (정보통신망법 제50조 — 수신 동의 확인 · 2년마다 재확인에 필요)
 */
@Getter
@Entity
@Table(name = "notification_setting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "chat_enabled", nullable = false)
    private boolean chatEnabled;

    @Column(name = "activity_enabled", nullable = false)
    private boolean activityEnabled;

    @Column(name = "favorite_space_enabled", nullable = false)
    private boolean favoriteSpaceEnabled;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled;

    /** 광고성 정보 수신 동의 시각. 동의하지 않았거나 철회했으면 null. */
    @Column(name = "marketing_consented_at")
    private LocalDateTime marketingConsentedAt;

    @Embedded
    private DoNotDisturb doNotDisturb;

    private NotificationSetting(Member member) {
        this.member = member;
    }

    /** 처음 저장할 때. 저장 전에는 광고성 정보에 동의하지 않은 상태로 본다. */
    public static NotificationSetting create(Member member, NotificationPreferences preferences, LocalDateTime now) {
        NotificationSetting setting = new NotificationSetting(member);
        setting.replace(preferences, now);
        return setting;
    }

    /**
     * 모든 값을 바꾼다. (PUT 전체 교체)
     * 광고성 정보는 동의하지 않음 → 동의로 바뀔 때만 동의 시각을 새로 남기고, 이미 동의 중이면 처음 동의한 시각을 유지한다.
     */
    public void replace(NotificationPreferences preferences, LocalDateTime now) {
        recordMarketingConsent(preferences.marketing(), now);
        this.pushEnabled = preferences.push();
        this.chatEnabled = preferences.chat();
        this.activityEnabled = preferences.activity();
        this.favoriteSpaceEnabled = preferences.favoriteSpace();
        this.marketingEnabled = preferences.marketing();
        this.doNotDisturb = preferences.doNotDisturb();
    }

    public NotificationPreferences preferences() {
        return new NotificationPreferences(pushEnabled, chatEnabled, activityEnabled, favoriteSpaceEnabled,
                marketingEnabled, doNotDisturb);
    }

    private void recordMarketingConsent(boolean consents, LocalDateTime now) {
        if (!consents) {
            this.marketingConsentedAt = null;
        } else if (!marketingEnabled) {
            this.marketingConsentedAt = now;
        }
    }
}
