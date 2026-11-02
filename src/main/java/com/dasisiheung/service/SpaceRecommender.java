package com.dasisiheung.service;

import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.UsePurpose;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 공간 추천 점수. 규칙이 단순하고 설명 가능하도록 항목별 점수와 이유를 함께 만든다.
 * <pre>
 * 목적에 맞는 상권 테마   +40
 * 평균 평점 (5점 만점)     최대 +30  (평점 × 6)
 * 찜 수                   최대 +15  (min(찜, 30) × 0.5)
 * 후기 수                 최대 +10  (min(후기, 10))
 * 사진 있음               +5
 * </pre>
 */
public final class SpaceRecommender {

    static final double PURPOSE_MATCH = 40;
    static final double RATING_WEIGHT = 6;
    static final int FAVORITE_CAP = 30;
    static final double FAVORITE_WEIGHT = 0.5;
    static final int REVIEW_CAP = 10;
    static final double PHOTO_BONUS = 5;

    private SpaceRecommender() {
    }

    /** @param purpose null 이면 목적 점수 없이 평점 · 인기만 본다 */
    public static Recommendation score(Space space, UsePurpose purpose) {
        double score = 0;
        List<String> reasons = new ArrayList<>();
        if (purpose != null && purpose.suits(space.getCommercialTheme())) {
            score += PURPOSE_MATCH;
            reasons.add("%s에 맞는 상권 (%s)".formatted(purpose.getLabel(), space.getCommercialTheme().getLabel()));
        }
        BigDecimal rating = space.getRatingAverage();
        if (rating != null) {
            score += rating.doubleValue() * RATING_WEIGHT;
            reasons.add(String.format(Locale.ROOT, "평점 %.1f (후기 %d개)", rating.doubleValue(), space.getReviewCount()));
        }
        score += Math.min(space.getReviewCount(), REVIEW_CAP);
        if (space.getFavoriteCount() > 0) {
            score += Math.min(space.getFavoriteCount(), FAVORITE_CAP) * FAVORITE_WEIGHT;
            reasons.add("찜 %d".formatted(space.getFavoriteCount()));
        }
        if (!space.getPhotos().isEmpty()) {
            score += PHOTO_BONUS;
        }
        return new Recommendation(space, Math.round(score * 10) / 10.0, List.copyOf(reasons));
    }

    /**
     * @param score   0~100
     * @param reasons 화면에 보여줄 추천 이유
     */
    public record Recommendation(Space space, double score, List<String> reasons) {
    }
}
