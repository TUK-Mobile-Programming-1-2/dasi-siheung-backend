package com.dasisiheung.service;

import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이용자의 공간 둘러보기 (로그인 없이도 가능). 공개(PUBLISHED) 공간만 보인다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceQueryService {

    private final SpaceRepository spaceRepository;
    private final SpaceResponseMapper responseMapper;
    private final Clock clock;

    /**
     * @throws BusinessException 없거나 공개 중이 아니면 SPACE_NOT_FOUND (검수 대기·숨김 공간의 존재를 드러내지 않는다)
     */
    public SpaceDetailResponse getSpace(Long spaceId) {
        Space space = getPublishedSpace(spaceId);
        return responseMapper.spaceDetail(space, LocalDate.now(clock));
    }

    private Space getPublishedSpace(Long spaceId) {
        return spaceRepository.findByIdAndStatus(spaceId, SpaceStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }
}
