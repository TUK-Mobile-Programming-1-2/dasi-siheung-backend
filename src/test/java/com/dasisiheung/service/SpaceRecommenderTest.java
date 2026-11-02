package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.UsePurpose;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SpaceRecommenderTest {

    @Test
    @DisplayName("목적에 맞는 테마 40 + 평점 × 6 + 후기 수 + 찜 × 0.5, 이유를 함께 준다")
    void score() {
        Space space = space(CommercialTheme.CAFE_STREET, new BigDecimal("4.50"), 4, 12);

        SpaceRecommender.Recommendation result = SpaceRecommender.score(space, UsePurpose.PROMOTION);

        assertThat(result.score()).isEqualTo(40 + 4.5 * 6 + 4 + 12 * 0.5);
        assertThat(result.reasons()).containsExactly("브랜드 홍보에 맞는 상권 (카페거리)", "평점 4.5 (후기 4개)", "찜 12");
    }

    @Test
    @DisplayName("목적이 없거나 맞지 않으면 목적 점수는 0, 후기 · 찜 없으면 이유도 없다")
    void noPurposeMatch() {
        Space space = space(CommercialTheme.INDUSTRIAL, null, 0, 0);

        assertThat(SpaceRecommender.score(space, UsePurpose.PORTFOLIO).score()).isZero();
        assertThat(SpaceRecommender.score(space, null).reasons()).isEmpty();
    }

    @Test
    @DisplayName("찜 · 후기 점수는 상한이 있다")
    void caps() {
        Space popular = space(null, null, 100, 1000);

        assertThat(SpaceRecommender.score(popular, null).score()).isEqualTo(10 + 30 * 0.5);
    }

    private static Space space(CommercialTheme theme, BigDecimal rating, int reviewCount, int favoriteCount) {
        Space space = SpaceFixtures.space(10L, SpaceFixtures.provider(2L), Set.of(), FixedClockConfig.FIXED_TODAY);
        space.correctTheme(theme);
        ReflectionTestUtils.setField(space, "ratingAverage", rating);
        ReflectionTestUtils.setField(space, "reviewCount", reviewCount);
        ReflectionTestUtils.setField(space, "favoriteCount", favoriteCount);
        return space;
    }
}
