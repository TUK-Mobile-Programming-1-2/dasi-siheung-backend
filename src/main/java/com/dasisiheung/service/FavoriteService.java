package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.FavoriteResponse;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.entity.space.Favorite;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.FavoriteRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공간 찜. 추가 · 해제 모두 여러 번 호출해도 결과가 같다(멱등). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final SpaceResponseMapper responseMapper;
    private final Clock clock;

    /** @throws BusinessException 공개 중이 아닌 공간이면 SPACE_NOT_FOUND */
    @Transactional
    public FavoriteResponse add(Long memberId, Long spaceId) {
        Space space = spaceRepository.findByIdAndStatus(spaceId, SpaceStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
        if (!favoriteRepository.existsByMember_IdAndSpace_Id(memberId, spaceId)) {
            favoriteRepository.save(new Favorite(memberRepository.getReferenceById(memberId), space,
                    LocalDateTime.now(clock)));
            spaceRepository.addFavoriteCount(spaceId, 1);
        }
        return new FavoriteResponse(spaceId, true, spaceRepository.findFavoriteCountById(spaceId));
    }

    /** 공개 상태와 상관없이 해제할 수 있다. */
    @Transactional
    public FavoriteResponse remove(Long memberId, Long spaceId) {
        Space space = spaceRepository.getSpace(spaceId);
        if (favoriteRepository.deleteByMemberIdAndSpaceId(memberId, spaceId) > 0) {
            spaceRepository.addFavoriteCount(space.getId(), -1);
        }
        return new FavoriteResponse(spaceId, false, spaceRepository.findFavoriteCountById(spaceId));
    }

    /** 내가 찜한 공개 공간 (최근 찜한 순). */
    public PageResponse<SpaceSummaryResponse> getMyFavorites(Long memberId, PageQuery pageQuery) {
        return PageResponse.from(favoriteRepository.findByMember_IdAndSpace_Status(memberId, SpaceStatus.PUBLISHED,
                        pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt", "id"))),
                favorite -> responseMapper.spaceSummary(favorite.getSpace()));
    }
}
