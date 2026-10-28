package com.dasisiheung.entity.space;

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

/** 공간 사진. 순서는 sortOrder 오름차순이고 첫 번째 사진이 대표 사진이다. */
@Getter
@Entity
@Table(name = "space_photo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "storage_path", nullable = false, length = 300)
    private String storagePath;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    SpacePhoto(Space space, String storagePath, int sortOrder, LocalDateTime createdAt) {
        this.space = space;
        this.storagePath = storagePath;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
