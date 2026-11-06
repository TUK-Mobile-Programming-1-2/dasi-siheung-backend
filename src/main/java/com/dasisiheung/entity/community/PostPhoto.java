package com.dasisiheung.entity.community;

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
 * 게시글 사진. 올린 순서(ID 오름차순)로 보여주고 첫 사진이 목록 썸네일이다.
 * 글과 따로 저장해 사진을 더하고 빼도 글의 버전(낙관적 잠금)이 바뀌지 않는다.
 */
@Getter
@Entity
@Table(name = "community_post_photo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "storage_path", nullable = false, length = 300)
    private String storagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostPhoto(Post post, String storagePath, LocalDateTime createdAt) {
        this.post = post;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
    }
}
