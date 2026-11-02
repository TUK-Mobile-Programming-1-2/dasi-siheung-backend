package com.dasisiheung.repository;

import com.dasisiheung.entity.review.UsageReview;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageReviewRepository extends JpaRepository<UsageReview, Long> {

    /** @throws BusinessException 없으면 REVIEW_NOT_FOUND */
    default UsageReview getReview(Long reviewId) {
        return findById(reviewId).orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    boolean existsByApplication_Id(Long applicationId);

    @EntityGraph(attributePaths = {"reviewer", "application"})
    Page<UsageReview> findBySpace_Id(Long spaceId, Pageable pageable);

    @EntityGraph(attributePaths = {"space", "application"})
    Page<UsageReview> findByReviewer_Id(Long reviewerId, Pageable pageable);
}
