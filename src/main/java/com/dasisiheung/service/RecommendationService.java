package com.dasisiheung.service;

import com.dasisiheung.dto.space.RecommendationResponse;
import com.dasisiheung.dto.space.SpaceSearchRequest;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.UsePurpose;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.SpaceRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이용 목적 · 활동 · 기간에 맞는 공간 추천.
 * 활동 · 기간은 반드시 만족해야 하는 조건으로 후보를 거르고, 목적 · 평점 · 인기로 점수를 매긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    /** 점수를 매길 후보 수 상한 (찜 많은 순으로 자른다). */
    static final int CANDIDATE_LIMIT = 300;
    public static final int MAX_RESULTS = 20;

    private final SpaceRepository spaceRepository;
    private final SpaceQueryService spaceQueryService;
    private final SpaceResponseMapper responseMapper;

    public List<RecommendationResponse> recommend(UsePurpose purpose, ActivityType activityType, LocalDate startDate,
                                                  LocalDate endDate, int limit) {
        SpaceSearchRequest filter = new SpaceSearchRequest(null, activityType, null, startDate, endDate, null, null,
                null, null, null, null, null);
        if (!filter.isPeriodValid()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "시작일과 종료일은 함께 보내야 하고, 종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (filter.hasPeriod()) {
            spaceQueryService.validatePeriod(startDate, endDate);
        }
        List<Space> candidates = spaceRepository.findAll(SpaceQueryService.toSpecification(filter),
                PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.DESC, "favoriteCount", "id"))).getContent();

        return candidates.stream()
                .map(space -> SpaceRecommender.score(space, purpose))
                .sorted(Comparator.comparingDouble(SpaceRecommender.Recommendation::score).reversed())
                .limit(Math.min(limit, MAX_RESULTS))
                .map(recommendation -> new RecommendationResponse(responseMapper.spaceSummary(recommendation.space()),
                        recommendation.score(), recommendation.reasons()))
                .toList();
    }
}
