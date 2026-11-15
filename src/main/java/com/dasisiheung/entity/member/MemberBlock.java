package com.dasisiheung.entity.member;

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

/**
 * 회원(blocker)이 다른 회원(blocked)을 차단한 한 건. 같은 상대는 한 번만 차단한다.
 * 한 사람이라도 상대를 차단했으면 두 사람은 서로 메시지를 보내거나 새 대화를 열 수 없다.
 */
@Getter
@Entity
@Table(name = "member_block")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private Member blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private Member blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private MemberBlock(Member blocker, Member blocked, LocalDateTime createdAt) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = createdAt;
    }

    /** @throws BusinessException 자기 자신을 차단하면 CANNOT_BLOCK_SELF */
    public static MemberBlock block(Member blocker, Member blocked, LocalDateTime now) {
        if (blocker.getId().equals(blocked.getId())) {
            throw new BusinessException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        return new MemberBlock(blocker, blocked, now);
    }
}
