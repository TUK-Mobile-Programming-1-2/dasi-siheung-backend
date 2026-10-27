package com.dasisiheung.entity.space;

import static com.dasisiheung.entity.space.CommercialTheme.CAFE_STREET;
import static com.dasisiheung.entity.space.CommercialTheme.CAMPUS_TOWN;
import static com.dasisiheung.entity.space.CommercialTheme.CHICKEN_ALLEY;
import static com.dasisiheung.entity.space.CommercialTheme.FOOD_ALLEY;
import static com.dasisiheung.entity.space.CommercialTheme.INDUSTRIAL;
import static com.dasisiheung.entity.space.CommercialTheme.RESIDENTIAL;
import static com.dasisiheung.entity.space.CommercialTheme.WATERFRONT;

import com.dasisiheung.entity.LabeledEnum;
import java.util.Set;
import lombok.Getter;

/**
 * 이용자가 공간을 찾는 목적. 목적마다 잘 맞는 상권 테마가 있다. (검색 필터 · 추천)
 */
@Getter
public enum UsePurpose implements LabeledEnum {

    SALES("판매·매출", Set.of(FOOD_ALLEY, CHICKEN_ALLEY, CAMPUS_TOWN, INDUSTRIAL)),
    PROMOTION("브랜드 홍보", Set.of(CAFE_STREET, CAMPUS_TOWN, WATERFRONT)),
    MARKET_TEST("창업 아이템 테스트", Set.of(CAMPUS_TOWN, RESIDENTIAL, FOOD_ALLEY)),
    PORTFOLIO("작품 발표·전시", Set.of(CAFE_STREET, WATERFRONT)),
    COMMUNITY("지역 교류", Set.of(RESIDENTIAL, CAMPUS_TOWN, WATERFRONT));

    private final String label;
    private final Set<CommercialTheme> suitableThemes;

    UsePurpose(String label, Set<CommercialTheme> suitableThemes) {
        this.label = label;
        this.suitableThemes = suitableThemes;
    }

    public boolean suits(CommercialTheme theme) {
        return theme != null && suitableThemes.contains(theme);
    }
}
