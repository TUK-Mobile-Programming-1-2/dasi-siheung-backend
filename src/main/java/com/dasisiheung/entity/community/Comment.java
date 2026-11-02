package com.dasisiheung.entity.community;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 게시글 댓글. */
@Getter
@Entity
@Table(name = "community_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    public Comment(Post post, Member author, String content) {
        this.post = post;
        this.author = author;
        this.content = content.strip();
        this.status = ContentStatus.VISIBLE;
    }

    public boolean isVisible() {
        return status == ContentStatus.VISIBLE;
    }

    /** 작성자 또는 관리자만 지울 수 있다. */
    public void validateDeletableBy(Long memberId, Role role) {
        if (role != Role.ADMIN && !author.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
    }

    public void hide() {
        this.status = ContentStatus.HIDDEN;
    }
}
