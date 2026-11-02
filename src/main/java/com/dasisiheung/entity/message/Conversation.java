package com.dasisiheung.entity.message;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 공간별 이용자 ↔ 공간 제공자 1:1 대화.
 * 두 사람이 동시에 보내고 읽어도 서로의 읽음 위치를 덮어쓰지 않도록 바뀐 컬럼만 UPDATE 한다.
 */
@Getter
@Entity
@DynamicUpdate
@Table(name = "conversation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseTimeEntity {

    private static final int PREVIEW_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Member provider;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = PREVIEW_LENGTH)
    private String lastMessagePreview;

    @Column(name = "user_last_read_message_id", nullable = false)
    private long userLastReadMessageId;

    @Column(name = "provider_last_read_message_id", nullable = false)
    private long providerLastReadMessageId;

    public Conversation(Space space, Member user) {
        this.space = space;
        this.user = user;
        this.provider = space.getProvider();
    }

    public boolean isUser(Long memberId) {
        return user.getId().equals(memberId);
    }

    public boolean isParticipant(Long memberId) {
        return isUser(memberId) || provider.getId().equals(memberId);
    }

    /** @throws BusinessException 대화 참여자가 아니면 NOT_CONVERSATION_MEMBER */
    public void validateParticipant(Long memberId) {
        if (!isParticipant(memberId)) {
            throw new BusinessException(ErrorCode.NOT_CONVERSATION_MEMBER);
        }
    }

    public Member counterpartOf(Long memberId) {
        return isUser(memberId) ? provider : user;
    }

    public long lastReadMessageIdOf(Long memberId) {
        return isUser(memberId) ? userLastReadMessageId : providerLastReadMessageId;
    }

    /** 보낸 메시지를 목록 미리보기에 반영하고, 보낸 사람은 그 메시지까지 읽은 것으로 한다. */
    public void onMessageSent(Message message) {
        this.lastMessageAt = message.getCreatedAt();
        String content = message.getContent();
        this.lastMessagePreview = content.length() <= PREVIEW_LENGTH ? content : content.substring(0, PREVIEW_LENGTH);
        markRead(message.getSender().getId(), message.getId());
    }

    /** 읽음 위치는 앞으로만 움직인다. */
    public void markRead(Long memberId, long messageId) {
        validateParticipant(memberId);
        if (isUser(memberId)) {
            userLastReadMessageId = Math.max(userLastReadMessageId, messageId);
        } else {
            providerLastReadMessageId = Math.max(providerLastReadMessageId, messageId);
        }
    }
}
