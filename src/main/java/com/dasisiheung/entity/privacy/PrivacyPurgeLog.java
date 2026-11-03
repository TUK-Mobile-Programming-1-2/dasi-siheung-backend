package com.dasisiheung.entity.privacy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 개인정보 파기 실행 기록. */
@Getter
@Entity
@Table(name = "privacy_purge_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivacyPurgeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 실행한 관리자 ID (자동 실행이면 null). */
    @Column(name = "executed_by")
    private Long executedBy;

    @Column(name = "application_cutoff", nullable = false)
    private LocalDate applicationCutoff;

    @Column(name = "counted_bookings", nullable = false)
    private int countedBookings;

    @Column(name = "anonymized_applications", nullable = false)
    private int anonymizedApplications;

    @Column(name = "deleted_conversations", nullable = false)
    private int deletedConversations;

    @Column(name = "deleted_activity_photos", nullable = false)
    private int deletedActivityPhotos;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    public PrivacyPurgeLog(Long executedBy, LocalDate applicationCutoff, int countedBookings,
                           int anonymizedApplications, int deletedConversations, int deletedActivityPhotos,
                           LocalDateTime executedAt) {
        this.executedBy = executedBy;
        this.applicationCutoff = applicationCutoff;
        this.countedBookings = countedBookings;
        this.anonymizedApplications = anonymizedApplications;
        this.deletedConversations = deletedConversations;
        this.deletedActivityPhotos = deletedActivityPhotos;
        this.executedAt = executedAt;
    }
}
