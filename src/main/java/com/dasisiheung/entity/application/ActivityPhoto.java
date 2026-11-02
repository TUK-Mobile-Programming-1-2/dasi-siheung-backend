package com.dasisiheung.entity.application;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 확정 예약의 활동 전후 사진 한 장. 신청자와 공간 제공자 모두 올리고 볼 수 있다. */
@Getter
@Entity
@Table(name = "activity_photo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityPhoto {

    /** 시점(전 · 후)별 최대 장수. */
    public static final int MAX_PER_PHASE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Member uploader;

    @Enumerated(EnumType.STRING)
    @Column(name = "uploader_side", nullable = false, length = 20)
    private OfferSide uploaderSide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PhotoPhase phase;

    @Column(name = "storage_path", nullable = false, length = 300)
    private String storagePath;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ActivityPhoto(Application application, Member uploader, OfferSide uploaderSide, PhotoPhase phase,
                          StoredPhotoFile file, LocalDateTime createdAt) {
        this.application = application;
        this.uploader = uploader;
        this.uploaderSide = uploaderSide;
        this.phase = phase;
        this.storagePath = file.path();
        this.originalName = file.originalName();
        this.contentType = file.contentType();
        this.fileSize = file.size();
        this.createdAt = createdAt;
    }

    /**
     * 올리기 전에 검사한다. (파일 업로드 전에 불러 실패 시 파일이 남지 않게 한다)
     *
     * @param currentCount 이 예약 · 시점에 이미 올린 장수
     * @throws BusinessException 참여자가 아니면 NOT_APPLICATION_PARTICIPANT, 확정 예약이 아니거나 기간 밖이면
     *                           ACTIVITY_PHOTO_NOT_ALLOWED, 장수 초과면 ACTIVITY_PHOTO_LIMIT_EXCEEDED
     */
    public static void validateUpload(Application application, Long uploaderId, PhotoPhase phase, int currentCount,
                                      int addingCount, LocalDate today) {
        application.sideOf(uploaderId);
        boolean open = application.getStatus() == ApplicationStatus.APPROVED
                && phase.isOpen(application.getStartDate(), application.getEndDate(), today);
        if (!open) {
            throw new BusinessException(ErrorCode.ACTIVITY_PHOTO_NOT_ALLOWED);
        }
        if (currentCount + addingCount > MAX_PER_PHASE) {
            throw new BusinessException(ErrorCode.ACTIVITY_PHOTO_LIMIT_EXCEEDED);
        }
    }

    public static ActivityPhoto of(Application application, Member uploader, PhotoPhase phase, StoredPhotoFile file,
                                   LocalDateTime now) {
        return new ActivityPhoto(application, uploader, application.sideOf(uploader.getId()), phase, file, now);
    }

    /** @throws BusinessException 올린 사람이 아니면 NOT_PHOTO_UPLOADER */
    public void validateUploader(Long memberId) {
        if (!uploader.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PHOTO_UPLOADER);
        }
    }

    /** 저장소에 올린 파일 정보. (엔티티가 서비스 계층 타입에 기대지 않도록 따로 둔다) */
    public record StoredPhotoFile(String path, String originalName, String contentType, long size) {
    }
}
