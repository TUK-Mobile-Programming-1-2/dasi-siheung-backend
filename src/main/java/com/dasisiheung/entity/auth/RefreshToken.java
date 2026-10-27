package com.dasisiheung.entity.auth;

import com.dasisiheung.entity.member.Member;
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
 * 리프레시 토큰. 원문은 클라이언트만 갖고, 서버에는 SHA-256 해시만 저장한다.
 * 재발급할 때마다 기존 토큰을 지우고 새로 발급한다. (Rotation)
 */
@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RefreshToken(Member member, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.createdAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(Member member, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        return new RefreshToken(member, tokenHash, issuedAt, expiresAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }
}
