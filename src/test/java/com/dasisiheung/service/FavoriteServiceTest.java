package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.space.FavoriteResponse;
import com.dasisiheung.entity.space.Favorite;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.FavoriteRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.ApplicationFixtures;
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
class FavoriteServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private MemberRepository memberRepository;

    private FavoriteService favoriteService;
    private Space space;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, spaceRepository, memberRepository,
                new SpaceResponseMapper(mock(FileUploadService.class)), FixedClockConfig.fixedClock());
        space = ApplicationFixtures.publishedSpace(
                SpaceFixtures.space(10L, SpaceFixtures.provider(2L), Set.of(), FixedClockConfig.FIXED_TODAY));
    }

    @Test
    @DisplayName("처음 찜하면 저장하고 찜 수를 1 더한다")
    void addFirstTime() {
        given(spaceRepository.findByIdAndStatus(10L, SpaceStatus.PUBLISHED)).willReturn(Optional.of(space));
        given(spaceRepository.findFavoriteCountById(10L)).willReturn(1);

        FavoriteResponse response = favoriteService.add(USER_ID, 10L);

        assertThat(response).isEqualTo(new FavoriteResponse(10L, true, 1));
        then(favoriteRepository).should().save(any(Favorite.class));
        then(spaceRepository).should().addFavoriteCount(10L, 1);
    }

    @Test
    @DisplayName("이미 찜했으면 저장 · 카운트를 건드리지 않는다 (멱등)")
    void addAgain() {
        given(spaceRepository.findByIdAndStatus(10L, SpaceStatus.PUBLISHED)).willReturn(Optional.of(space));
        given(favoriteRepository.existsByMember_IdAndSpace_Id(USER_ID, 10L)).willReturn(true);

        favoriteService.add(USER_ID, 10L);

        then(favoriteRepository).should(never()).save(any());
        then(spaceRepository).should(never()).addFavoriteCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("공개 중이 아닌 공간은 SPACE_NOT_FOUND")
    void notPublished() {
        given(spaceRepository.findByIdAndStatus(10L, SpaceStatus.PUBLISHED)).willReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.add(USER_ID, 10L))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_NOT_FOUND);
    }

    @Test
    @DisplayName("해제: 지운 행이 있을 때만 찜 수를 1 뺀다")
    void remove() {
        given(spaceRepository.getSpace(10L)).willReturn(space);
        given(favoriteRepository.deleteByMemberIdAndSpaceId(USER_ID, 10L)).willReturn(1, 0);

        favoriteService.remove(USER_ID, 10L);
        favoriteService.remove(USER_ID, 10L);

        then(spaceRepository).should().addFavoriteCount(10L, -1);
    }
}
