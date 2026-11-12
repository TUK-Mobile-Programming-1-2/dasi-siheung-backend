package com.dasisiheung.entity.member;

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

/** 회원 기기 하나의 FCM 토큰. 한 토큰은 한 회원에게만 속한다. */
@Getter
@Entity
@Table(name = "push_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken {

    public static final int MAX_TOKEN_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = MAX_TOKEN_LENGTH)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_registered_at", nullable = false)
    private LocalDateTime lastRegisteredAt;

    private PushToken(Member member, String token, LocalDateTime now) {
        this.member = member;
        this.token = token;
        this.createdAt = now;
        this.lastRegisteredAt = now;
    }

    public static PushToken register(Member member, String token, LocalDateTime now) {
        return new PushToken(member, token, now);
    }

    /** 다시 등록됐다. 같은 기기에서 다른 계정으로 로그인했으면 그 회원에게 옮긴다. */
    public void renew(Member member, LocalDateTime now) {
        this.member = member;
        this.lastRegisteredAt = now;
    }
}
