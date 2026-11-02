package com.dasisiheung.repository;

import com.dasisiheung.entity.community.Comment;
import com.dasisiheung.entity.community.ContentStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** @throws BusinessException 없으면 COMMENT_NOT_FOUND */
    default Comment getComment(Long commentId) {
        return findById(commentId).orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    @EntityGraph(attributePaths = "author")
    Page<Comment> findByPost_IdAndStatus(Long postId, ContentStatus status, Pageable pageable);
}
