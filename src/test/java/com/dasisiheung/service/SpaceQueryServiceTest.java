package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.dto.space.SpaceSearchRequest;
import com.dasisiheung.dto.space.SpaceSort;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SpaceQueryServiceTest {

    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;

    @Mock
    private SpaceRepository spaceRepository;

    private SpaceQueryService spaceQueryService;

    @BeforeEach
    void setUp() {
        spaceQueryService = new SpaceQueryService(spaceRepository,
                new SpaceResponseMapper(mock(FileUploadService.class)), FixedClockConfig.fixedClock());
    }

    @Test
    @DisplayName("지난 날짜는 빼고 오늘부터 오름차순으로 보여준다")
    void upcomingDatesOnly() {
        Space space = SpaceFixtures.space(10L, SpaceFixtures.provider(2L),
                Set.of(TODAY, TODAY.plusDays(3), TODAY.plusDays(1)), TODAY);
        space.getAvailableDates().add(TODAY.minusDays(1)); // 등록 뒤 시간이 지나 과거가 된 날짜
        given(spaceRepository.findByIdAndStatus(10L, SpaceStatus.PUBLISHED)).willReturn(Optional.of(space));

        SpaceDetailResponse response = spaceQueryService.getSpace(10L);

        assertThat(response.availableDates()).containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(3));
        assertThat(response.provider().id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("공개 중이 아니면 SPACE_NOT_FOUND")
    void notPublished() {
        given(spaceRepository.findByIdAndStatus(10L, SpaceStatus.PUBLISHED)).willReturn(Optional.empty());

        assertThatThrownBy(() -> spaceQueryService.getSpace(10L))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("검색: 정렬 기준에 ID 내림차순을 덧붙여 페이지를 요청한다")
    @SuppressWarnings("unchecked")
    void searchSort() {
        Space space = SpaceFixtures.space(10L, SpaceFixtures.provider(2L), Set.of(), TODAY);
        given(spaceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(space)));
        SpaceSearchRequest request = new SpaceSearchRequest(null, null, null, null, null, null, null, null, null,
                SpaceSort.PRICE_ASC);

        PageResponse<SpaceSummaryResponse> response = spaceQueryService.search(request, new PageQuery(0, 20));

        assertThat(response.content()).extracting(SpaceSummaryResponse::id).containsExactly(10L);
        then(spaceRepository).should().findAll(any(Specification.class), argThat((Pageable pageable) ->
                pageable.getSort().equals(Sort.by(Sort.Direction.ASC, "dailyPrice").and(Sort.by(Sort.Direction.DESC, "id")))));
    }

    @Test
    @DisplayName("검색: 기간이 오늘 이전이거나 90일을 넘으면 INVALID_SEARCH_PERIOD 이고 조회하지 않는다")
    @SuppressWarnings("unchecked")
    void invalidPeriod() {
        assertThatThrownBy(() -> spaceQueryService.search(period(TODAY.minusDays(1), TODAY), new PageQuery(0, 20)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SEARCH_PERIOD);
        assertThatThrownBy(() -> spaceQueryService.search(period(TODAY, TODAY.plusDays(91)), new PageQuery(0, 20)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_SEARCH_PERIOD);
        then(spaceRepository).should(never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("검색 조건 기본값: 정렬 LATEST, 빈 시설, 공백 검색어는 없음")
    void defaults() {
        SpaceSearchRequest request = new SpaceSearchRequest(null, null, null, null, null, null, null, null, "  ", null);

        assertThat(request.sort()).isEqualTo(SpaceSort.LATEST);
        assertThat(request.facilities()).isEmpty();
        assertThat(request.keyword()).isNull();
        assertThat(request.isPeriodValid()).isTrue();
        assertThat(period(TODAY, null).isPeriodValid()).isFalse();
        assertThat(period(TODAY, TODAY.minusDays(1)).isPeriodValid()).isFalse();
    }

    private static SpaceSearchRequest period(LocalDate startDate, LocalDate endDate) {
        return new SpaceSearchRequest(null, null, null, startDate, endDate, null, null, null, null, null);
    }
}
