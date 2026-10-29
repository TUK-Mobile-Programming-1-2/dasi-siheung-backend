package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
