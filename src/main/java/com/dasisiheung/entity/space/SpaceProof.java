package com.dasisiheung.entity.space;

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

/** 공간 제공 권한 증빙. 비공개 저장소에 있고 관리자 검수 때 임시 주소로 연다. */
@Getter
@Entity
@Table(name = "space_proof")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 30)
    private ProofType proofType;

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

    SpaceProof(Space space, ProofType proofType, StoredProofFile file, LocalDateTime createdAt) {
        this.space = space;
        this.proofType = proofType;
        this.storagePath = file.storagePath();
        this.originalName = file.originalName();
        this.contentType = file.contentType();
        this.fileSize = file.size();
        this.createdAt = createdAt;
    }

    /**
     * 저장소에 올린 증빙 파일 정보.
     *
     * @param originalName 사용자가 올린 파일 이름 (최대 255자로 자름)
     */
    public record StoredProofFile(String storagePath, String originalName, String contentType, long size) {

        private static final int MAX_NAME_LENGTH = 255;

        public StoredProofFile {
            if (originalName != null && originalName.length() > MAX_NAME_LENGTH) {
                originalName = originalName.substring(0, MAX_NAME_LENGTH);
            }
        }
    }
}
