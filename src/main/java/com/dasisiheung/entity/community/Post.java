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
import jakarta.persistence.Version;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 커뮤니티 게시글. 댓글 수 · 조회 수는 저장소 UPDATE 로만 바뀐다. */
@Getter
@Entity
@Table(name = "community_post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    /** 목록 미리보기 최대 글자 수. (이모지 등도 한 글자) */
    public static final int PREVIEW_LENGTH = 100;

    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "comment_count", nullable = false, insertable = false, updatable = false)
    private int commentCount;

    @Column(name = "view_count", nullable = false, insertable = false, updatable = false)
    private int viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    private Post(Member author, PostCategory category, String title, String content) {
        this.author = author;
        this.status = ContentStatus.VISIBLE;
        apply(category, title, content);
    }

    /** @throws BusinessException 관리자가 아닌데 공지를 쓰면 NOTICE_ADMIN_ONLY */
    public static Post write(Member author, PostCategory category, String title, String content) {
        validateCategory(author.getRole(), category);
        return new Post(author, category, title, content);
    }

    public void edit(Long memberId, PostCategory category, String title, String content) {
        validateAuthor(memberId);
        validateCategory(author.getRole(), category);
        apply(category, title, content);
    }

    public boolean isVisible() {
        return status == ContentStatus.VISIBLE;
    }

    /** 목록 한 줄 요약: 줄바꿈 · 연속 공백을 공백 하나로 줄인 본문의 앞 100자. */
    public String contentPreview() {
        String flat = WHITESPACES.matcher(content).replaceAll(" ");
        if (flat.codePointCount(0, flat.length()) <= PREVIEW_LENGTH) {
            return flat;
        }
        return flat.substring(0, flat.offsetByCodePoints(0, PREVIEW_LENGTH));
    }

    public boolean isWrittenBy(Long memberId) {
        return author.getId().equals(memberId);
    }

    /** 작성자 또는 관리자만 지울 수 있다. */
    public void validateDeletableBy(Long memberId, Role role) {
        if (role != Role.ADMIN) {
            validateAuthor(memberId);
        }
    }

    /** 신고 처리로 숨긴다. */
    public void hide() {
        this.status = ContentStatus.HIDDEN;
    }

    private void validateAuthor(Long memberId) {
        if (!isWrittenBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_POST_AUTHOR);
        }
    }

    private static void validateCategory(Role role, PostCategory category) {
        if (category.isAdminOnly() && role != Role.ADMIN) {
            throw new BusinessException(ErrorCode.NOTICE_ADMIN_ONLY);
        }
    }

    private void apply(PostCategory category, String title, String content) {
        this.category = category;
        this.title = title.strip();
        this.content = content.strip();
    }
}
