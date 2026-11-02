package com.dasisiheung.repository;

import com.dasisiheung.entity.community.ContentStatus;
import com.dasisiheung.entity.community.Post;
import com.dasisiheung.entity.community.PostCategory;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** @throws BusinessException 없으면 POST_NOT_FOUND */
    default Post getPost(Long postId) {
        return findById(postId).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 보이는 게시글 목록.
     *
     * @param category null 이면 전체
     * @param keyword  null 이면 전체, 아니면 제목 · 본문 포함 (소문자, % _ 이스케이프된 패턴)
     */
    @EntityGraph(attributePaths = "author")
    @Query(value = """
            select p from Post p
            where p.status = :status
              and (:category is null or p.category = :category)
              and (:pattern is null or lower(p.title) like :pattern escape '\\' or lower(p.content) like :pattern escape '\\')
            """, countQuery = """
            select count(p) from Post p
            where p.status = :status
              and (:category is null or p.category = :category)
              and (:pattern is null or lower(p.title) like :pattern escape '\\' or lower(p.content) like :pattern escape '\\')
            """)
    Page<Post> search(@Param("status") ContentStatus status, @Param("category") PostCategory category,
                      @Param("pattern") String pattern, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId")
    int increaseViewCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Post p set p.commentCount = p.commentCount + :delta where p.id = :postId and p.commentCount + :delta >= 0")
    int addCommentCount(@Param("postId") Long postId, @Param("delta") int delta);
}
