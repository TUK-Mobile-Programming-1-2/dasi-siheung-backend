package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.ServiceRegionRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    private static final long PROVIDER_ID = 2L;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ServiceRegionRepository serviceRegionRepository;

    private SpaceService spaceService;

    @BeforeEach
    void setUp() {
        spaceService = new SpaceService(spaceRepository, memberRepository, serviceRegionRepository,
                FixedClockConfig.fixedClock());
    }

    @Test
    @DisplayName("좌표로 판정한 지역과 검수 대기 상태로 저장한다")
    void register() {
        given(memberRepository.findById(PROVIDER_ID)).willReturn(Optional.of(SpaceFixtures.provider(PROVIDER_ID)));
        given(serviceRegionRepository.findRegionCodeContaining(SpaceFixtures.BAEGOT_LAT, SpaceFixtures.BAEGOT_LNG))
                .willReturn(Optional.of("BAEGOT"));
        given(spaceRepository.save(any(Space.class))).willAnswer(invocation -> invocation.getArgument(0));

        ProviderSpaceResponse response = spaceService.register(PROVIDER_ID, SpaceFixtures.request(
                SpaceFixtures.BAEGOT_LAT, SpaceFixtures.BAEGOT_LNG, Set.of(FixedClockConfig.FIXED_TODAY.plusDays(1))));

        assertThat(response.region()).isEqualTo(Region.BAEGOT);
        assertThat(response.status()).isEqualTo(SpaceStatus.PENDING_REVIEW);
        assertThat(response.latitude()).isEqualTo(SpaceFixtures.BAEGOT_LAT);
        assertThat(response.availableDates()).containsExactly(FixedClockConfig.FIXED_TODAY.plusDays(1));
    }

    @Test
    @DisplayName("서비스 지역 밖이면 SPACE_OUT_OF_SERVICE_AREA 이고 저장하지 않는다")
    void outOfServiceArea() {
        given(memberRepository.findById(PROVIDER_ID)).willReturn(Optional.of(SpaceFixtures.provider(PROVIDER_ID)));
        given(serviceRegionRepository.findRegionCodeContaining(anyDouble(), anyDouble())).willReturn(Optional.empty());

        assertThatThrownBy(() -> spaceService.register(PROVIDER_ID,
                SpaceFixtures.request(SpaceFixtures.SEOUL_LAT, SpaceFixtures.SEOUL_LNG, Set.of())))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_OUT_OF_SERVICE_AREA);
        then(spaceRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("과거 날짜가 섞여 있으면 INVALID_AVAILABLE_DATE")
    void pastDate() {
        given(memberRepository.findById(PROVIDER_ID)).willReturn(Optional.of(SpaceFixtures.provider(PROVIDER_ID)));
        given(serviceRegionRepository.findRegionCodeContaining(anyDouble(), anyDouble())).willReturn(Optional.of("JEONGWANG"));

        assertThatThrownBy(() -> spaceService.register(PROVIDER_ID,
                SpaceFixtures.request(Set.of(FixedClockConfig.FIXED_TODAY.minusDays(1)))))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AVAILABLE_DATE);
    }
}
