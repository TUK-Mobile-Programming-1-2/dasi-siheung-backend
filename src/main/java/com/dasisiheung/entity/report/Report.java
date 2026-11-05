package com.dasisiheung.entity.report;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
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

/** 신고 한 건. 대상은 종류 + ID 로 가리킨다. */
@Getter
@Entity
@Table(name = "report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    /** 신고 한 건에 붙일 수 있는 증거 사진 최대 장수. */
    public static final int MAX_PHOTOS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReportAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Member admin;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * @param targetOwnerId 대상의 작성자 · 제공자 (회원 신고면 그 회원)
     * @throws BusinessException 자기 자신 · 자기 글을 신고하면 CANNOT_REPORT_SELF
     */
    public Report(Member reporter, ReportTargetType targetType, Long targetId, Long targetOwnerId,
                  ReportReason reason, String detail) {
        if (reporter.getId().equals(targetOwnerId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
        }
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail == null || detail.isBlank() ? null : detail.strip();
        this.status = ReportStatus.PENDING;
    }

    /**
     * 사진을 올리기 전에 장수를 검사한다. (파일 업로드 전에 불러 실패 시 파일이 남지 않게 한다)
     *
     * @throws BusinessException 최대 장수를 넘으면 REPORT_PHOTO_LIMIT_EXCEEDED
     */
    public static void validatePhotoCount(int count) {
        if (count > MAX_PHOTOS) {
            throw new BusinessException(ErrorCode.REPORT_PHOTO_LIMIT_EXCEEDED);
        }
    }

    /**
     * 관리자 처리 결과를 남긴다. (숨김 · 정지 자체는 서비스가 대상에 적용한다)
     *
     * @throws BusinessException 이미 처리됐으면 REPORT_ALREADY_RESOLVED, 숨길 수 없는 대상에 숨김이면 INVALID_REPORT_ACTION
     */
    public void resolve(Member admin, ReportAction action, String note, LocalDateTime now) {
        if (status != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        if (action.isHidesTarget() && !targetType.isHideable()) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_ACTION);
        }
        this.admin = admin;
        this.action = action;
        this.status = action.resultStatus();
        this.adminNote = note == null || note.isBlank() ? null : note.strip();
        this.resolvedAt = now;
    }
}
