package com.dasisiheung.entity.space;

import com.dasisiheung.entity.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

/** 관리자 검수 이력 한 건. */
@Getter
@Entity
@Table(name = "space_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Member admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(length = 1000)
    private String reason;

    @Embedded
    private ReviewChecklist checklist;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    SpaceReview(Space space, Member admin, ReviewDecision decision, String reason, ReviewChecklist checklist,
                LocalDateTime createdAt) {
        this.space = space;
        this.admin = admin;
        this.decision = decision;
        this.reason = reason;
        this.checklist = checklist;
        this.createdAt = createdAt;
    }
}
