package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.review.ReviewReplyRequest;
import com.dasisiheung.dto.review.ReviewRequest;
import com.dasisiheung.dto.review.ReviewResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.review.UsageReview;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.repository.UsageReviewRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용 후기 작성 · 수정 · 삭제 · 답글 · 조회. 공간의 후기 수 · 평점 합계를 함께 갱신한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final UsageReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final SpaceRepository spaceRepository;
    private final Clock clock;

    /** @throws BusinessException 이미 썼으면 DUPLICATE_REVIEW, 그 밖은 {@link UsageReview#write} 참고 */
    @Transactional
    public ReviewResponse write(Long reviewerId, Long applicationId, ReviewRequest request) {
        Application application = applicationRepository.getApplication(applicationId);
        UsageReview review = UsageReview.write(application, reviewerId, request.rating(), request.content(),
                LocalDate.now(clock));
        if (reviewRepository.existsByApplication_Id(applicationId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }
        reviewRepository.saveAndFlush(review);
        spaceRepository.addReviewStats(application.getSpace().getId(), 1, review.getRating());
        return ReviewResponse.from(reviewRepository.getReview(review.getId()));
    }

    @Transactional
    public ReviewResponse edit(Long reviewerId, Long reviewId, ReviewRequest request) {
        UsageReview review = reviewRepository.getReview(reviewId);
        int ratingDelta = review.edit(reviewerId, request.rating(), request.content());
        reviewRepository.flush();
        if (ratingDelta != 0) {
            spaceRepository.addReviewStats(review.getSpace().getId(), 0, ratingDelta);
            review = reviewRepository.getReview(reviewId);
        }
        return ReviewResponse.from(review);
    }

    @Transactional
    public void delete(Long reviewerId, Long reviewId) {
        UsageReview review = reviewRepository.getReview(reviewId);
        review.validateOwner(reviewerId);
        Long spaceId = review.getSpace().getId();
        int rating = review.getRating();
        reviewRepository.delete(review);
        reviewRepository.flush();
        spaceRepository.addReviewStats(spaceId, -1, -rating);
    }

    @Transactional
    public ReviewResponse reply(Long providerId, Long reviewId, ReviewReplyRequest request) {
        UsageReview review = reviewRepository.getReview(reviewId);
        review.reply(providerId, request.content(), LocalDateTime.now(clock));
        reviewRepository.flush();
        return ReviewResponse.from(review);
    }

    /** 공간의 후기 (최근 순). 공개 공간만 볼 수 있다. */
    public PageResponse<ReviewResponse> getSpaceReviews(Long spaceId, PageQuery pageQuery) {
        spaceRepository.findByIdAndStatus(spaceId, SpaceStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
        return PageResponse.from(reviewRepository.findBySpace_Id(spaceId, pageQuery.toPageable(LATEST_FIRST)),
                ReviewResponse::from);
    }

    public PageResponse<ReviewResponse> getMyReviews(Long reviewerId, PageQuery pageQuery) {
        return PageResponse.from(reviewRepository.findByReviewer_Id(reviewerId, pageQuery.toPageable(LATEST_FIRST)),
                ReviewResponse::from);
    }
}
