package com.dasisiheung.entity.inquiry;

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

/** 1:1 문의 첨부 사진 한 장. 비공개 저장소에 두고 볼 때마다 임시 주소를 발급한다. */
@Getter
@Entity
@Table(name = "inquiry_photo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

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

    public InquiryPhoto(Inquiry inquiry, StoredPhotoFile file, LocalDateTime createdAt) {
        this.inquiry = inquiry;
        this.storagePath = file.path();
        this.originalName = file.originalName();
        this.contentType = file.contentType();
        this.fileSize = file.size();
        this.createdAt = createdAt;
    }

    /** 저장소에 올린 파일 정보. (엔티티가 서비스 계층 타입에 기대지 않도록 따로 둔다) */
    public record StoredPhotoFile(String path, String originalName, String contentType, long size) {
    }
}
