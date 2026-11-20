package com.dasisiheung.entity.application;

import com.dasisiheung.entity.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 가격 제안 한 건. 상대가 수락하면 acceptedAt, 거절하면 rejectedAt 이 채워진다. (둘 중 하나만) */
@Getter
@Entity
@Table(name = "price_offer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceOffer {

    /** 제안 · 거절 한마디 최대 글자 수. */
    public static final int MAX_MESSAGE_LENGTH = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposer_id", nullable = false)
    private Member proposer;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposer_side", nullable = false, length = 20)
    private OfferSide proposerSide;

    @Column(nullable = false)
    private int amount;

    @Column(length = MAX_MESSAGE_LENGTH)
    private String message;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_message", length = MAX_MESSAGE_LENGTH)
    private String rejectionMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    PriceOffer(Application application, Member proposer, OfferSide proposerSide, int amount, String message,
               LocalDateTime createdAt) {
        this.application = application;
        this.proposer = proposer;
        this.proposerSide = proposerSide;
        this.amount = amount;
        this.message = message;
        this.createdAt = createdAt;
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public boolean isRejected() {
        return rejectedAt != null;
    }

    void accept(LocalDateTime now) {
        this.acceptedAt = now;
    }

    void reject(String message, LocalDateTime now) {
        this.rejectedAt = now;
        this.rejectionMessage = message;
    }
}
