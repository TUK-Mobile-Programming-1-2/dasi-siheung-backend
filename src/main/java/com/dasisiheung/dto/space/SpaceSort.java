package com.dasisiheung.dto.space;

import com.dasisiheung.entity.LabeledEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

/** 공간 검색 정렬. 같은 값이면 최근 공간이 먼저 온다. */
@Getter
@RequiredArgsConstructor
public enum SpaceSort implements LabeledEnum {

    LATEST("최근 공개 순", Sort.by(Sort.Direction.DESC, "reviewedAt")),
    PRICE_ASC("낮은 가격 순", Sort.by(Sort.Direction.ASC, "dailyPrice")),
    PRICE_DESC("높은 가격 순", Sort.by(Sort.Direction.DESC, "dailyPrice")),
    AREA_DESC("넓은 순", Sort.by(Sort.Direction.DESC, "areaSqm")),
    POPULAR("찜 많은 순", Sort.by(Sort.Direction.DESC, "favoriteCount"));

    private static final Sort TIE_BREAKER = Sort.by(Sort.Direction.DESC, "id");

    private final String label;
    private final Sort sort;

    public Sort toSort() {
        return sort.and(TIE_BREAKER);
    }
}
