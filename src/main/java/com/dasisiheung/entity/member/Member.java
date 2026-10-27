package com.dasisiheung.entity.member;

import com.dasisiheung.entity.BaseTimeEntity;
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
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    private Member(String email, String passwordHash, String name, String phone, Role role) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.name = name.strip();
        this.phone = normalizePhone(phone);
        this.role = role;
        this.status = MemberStatus.ACTIVE;
    }

    /**
     * 회원가입. 관리자 역할은 스스로 가입할 수 없다.
     *
     * @param passwordHash 이미 암호화된 비밀번호
     */
    public static Member signUp(String email, String passwordHash, String name, String phone, Role role) {
        if (!role.isSelfRegistrable()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_ALLOWED);
        }
        return new Member(email, passwordHash, name, phone, role);
    }

    /** 이메일은 대소문자를 구분하지 않도록 소문자로 저장한다. */
    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /** 휴대폰 번호는 숫자만 저장한다. (010-1234-5678 → 01012345678) */
    public static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }

    /**
     * 로그인·토큰 재발급 가능 여부를 확인한다. 정지 기간이 끝났으면 자동으로 정상 상태로 되돌린다.
     */
    public void validateCanSignIn(LocalDateTime now) {
        if (status == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (status == MemberStatus.SUSPENDED) {
            if (suspendedUntil != null && !now.isBefore(suspendedUntil)) {
                liftSuspension();
                return;
            }
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED, suspensionMessage());
        }
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public void liftSuspension() {
        this.status = MemberStatus.ACTIVE;
        this.suspendedUntil = null;
    }

    private String suspensionMessage() {
        if (suspendedUntil == null) {
            return ErrorCode.ACCOUNT_SUSPENDED.getMessage();
        }
        return "%s (해제 예정: %s)".formatted(ErrorCode.ACCOUNT_SUSPENDED.getMessage(), suspendedUntil.toLocalDate());
    }
}
