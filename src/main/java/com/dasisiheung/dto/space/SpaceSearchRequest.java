package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.UsePurpose;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import com.dasisiheung.entity.space.Space;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 공간 검색 조건 (쿼리 파라미터). 모든 조건은 선택이며 함께 보내면 모두 만족하는 공간만 나온다.
 */
public record SpaceSearchRequest(
        @Schema(description = "서비스 지역", example = "JEONGWANG")
        Region region,

        @Schema(description = "하고 싶은 활동", example = "POPUP_STORE")
        ActivityType activityType,

        @Schema(description = "이용 목적 — 목적에 맞는 상권 테마의 공간만", example = "PROMOTION")
        UsePurpose purpose,

        @Schema(description = "이용 시작일 yyyy-MM-dd (endDate 와 함께)", example = "2026-10-24")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @Schema(description = "이용 종료일 yyyy-MM-dd — 기간의 모든 날짜가 비어 있는 공간만", example = "2026-10-25")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        @Schema(description = "1일 이용료 하한(원)", example = "30000")
        @Min(0) Integer minPrice,

        @Schema(description = "1일 이용료 상한(원) — 예산", example = "80000")
        @Min(0) Integer maxPrice,

        @Schema(description = "꼭 있어야 하는 시설 (모두 갖춘 공간만)")
        Set<Facility> facilities,

        @Schema(description = "운영 인원 — 이 인원을 수용할 수 있는 공간만 (수용 인원을 정한 공간 중에서)", example = "8")
        @Min(1) @Max(Space.MAX_CAPACITY) Integer headcount,

        @Schema(description = "true 면 가격 협의가 가능한 공간만", example = "true")
        Boolean negotiableOnly,

        @Schema(description = "공간 이름 · 주소 검색어", example = "정왕역")
        @Size(max = 50) String keyword,

        @Schema(description = "정렬 (기본 LATEST)", example = "PRICE_ASC")
        SpaceSort sort
) {

    public SpaceSearchRequest {
        facilities = facilities == null ? Set.of() : Set.copyOf(facilities);
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        sort = sort == null ? SpaceSort.LATEST : sort;
    }

    public static SpaceSearchRequest empty() {
        return new SpaceSearchRequest(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public boolean hasPeriod() {
        return startDate != null && endDate != null;
    }

    @Parameter(hidden = true)
    @AssertTrue(message = "시작일과 종료일은 함께 보내야 하고, 종료일은 시작일보다 빠를 수 없습니다.")
    public boolean isPeriodValid() {
        if (startDate == null || endDate == null) {
            return startDate == null && endDate == null;
        }
        return !endDate.isBefore(startDate);
    }

    @Parameter(hidden = true)
    @AssertTrue(message = "가격 하한이 상한보다 클 수 없습니다.")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice <= maxPrice;
    }
}
