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

/** 가격 제안 한 건. 상대가 수락하면 acceptedAt 이 채워진다. */
@Getter
@Entity
@Table(name = "price_offer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceOffer {

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

    @Column(length = 300)
    private String message;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

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

    void accept(LocalDateTime now) {
        this.acceptedAt = now;
    }
}
