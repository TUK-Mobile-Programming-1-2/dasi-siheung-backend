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

    /** 탈퇴 회원의 이메일. ".invalid" 는 실제로 존재할 수 없는 예약 도메인이다. */
    private static final String WITHDRAWN_EMAIL_FORMAT = "withdrawn-%d@deleted.invalid";
    /** BCrypt 형식이 아니므로 어떤 비밀번호와도 일치하지 않는다. */
    private static final String WITHDRAWN_PASSWORD_HASH = "WITHDRAWN";
    private static final String WITHDRAWN_NAME = "탈퇴한 회원";

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

    /** 프로필 사진의 저장소 경로 (공개 버킷). 없으면 null. */
    @Column(name = "profile_image_path", length = 300)
    private String profileImagePath;

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

    /** 이름·연락처 수정. null 인 값은 바꾸지 않는다. */
    public void updateProfile(String name, String phone) {
        if (name != null) {
            this.name = name.strip();
        }
        if (phone != null) {
            this.phone = normalizePhone(phone);
        }
    }

    /**
     * 프로필 사진을 바꾼다.
     *
     * @param storagePath 새 사진의 저장소 경로
     * @return 바꾸기 전 사진 경로 (저장소에서 지울 대상, 없었으면 null)
     */
    public String changeProfileImage(String storagePath) {
        String previous = this.profileImagePath;
        this.profileImagePath = storagePath;
        return previous;
    }

    /**
     * 프로필 사진을 지우고 기본 이미지로 돌아간다.
     *
     * @return 지운 사진 경로 (저장소에서 지울 대상, 없었으면 null)
     */
    public String removeProfileImage() {
        return changeProfileImage(null);
    }

    /** @param newPasswordHash 이미 암호화된 새 비밀번호 */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * 탈퇴. 다시 로그인할 수 없도록 개인정보(프로필 사진 포함)를 즉시 지우고 식별할 수 없는 값으로 바꾼다.
     * 공간·신청 같은 거래 기록은 회원 ID 로만 남는다. 사진 파일은 호출하는 쪽이 {@link #removeProfileImage()} 로
     * 경로를 받아 저장소에서 지운다.
     */
    public void withdraw(LocalDateTime now) {
        this.email = WITHDRAWN_EMAIL_FORMAT.formatted(id);
        this.passwordHash = WITHDRAWN_PASSWORD_HASH;
        this.name = WITHDRAWN_NAME;
        this.phone = null;
        this.profileImagePath = null;
        this.status = MemberStatus.WITHDRAWN;
        this.suspendedUntil = null;
        this.withdrawnAt = now;
    }

    public boolean isWithdrawn() {
        return status == MemberStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    /**
     * 이용 정지. 정지 기간이 끝난 뒤 처음 로그인할 때 자동으로 풀린다.
     *
     * @throws BusinessException 관리자 · 탈퇴 회원이면 CANNOT_SUSPEND_MEMBER
     */
    public void suspend(LocalDateTime until) {
        if (role == Role.ADMIN || status == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.CANNOT_SUSPEND_MEMBER);
        }
        this.status = MemberStatus.SUSPENDED;
        this.suspendedUntil = until;
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
