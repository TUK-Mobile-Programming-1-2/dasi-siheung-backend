package com.dasisiheung.repository;

import com.dasisiheung.entity.review.UsageReview;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 공간 제공자가 받은 후기 (내 모든 공간).
     *
     * @param spaceId null 이면 내 모든 공간
     * @param replied null 이면 전체, true 면 답글 단 것, false 면 답글을 기다리는 것
     */
    @EntityGraph(attributePaths = {"space", "reviewer", "application"})
    @Query(value = """
            select r from UsageReview r
            where r.space.provider.id = :providerId
              and (:spaceId is null or r.space.id = :spaceId)
              and (:replied is null or (:replied = true and r.reply is not null) or (:replied = false and r.reply is null))
            """, countQuery = """
            select count(r) from UsageReview r
            where r.space.provider.id = :providerId
              and (:spaceId is null or r.space.id = :spaceId)
              and (:replied is null or (:replied = true and r.reply is not null) or (:replied = false and r.reply is null))
            """)
    Page<UsageReview> findReceived(@Param("providerId") Long providerId, @Param("spaceId") Long spaceId,
                                   @Param("replied") Boolean replied, Pageable pageable);
}
