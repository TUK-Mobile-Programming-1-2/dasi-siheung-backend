package com.dasisiheung.service;

import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.ServiceRegionRepository;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공간 제공자의 공간 등록 · 관리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final MemberRepository memberRepository;
    private final ServiceRegionRepository serviceRegionRepository;
    private final SpaceResponseMapper responseMapper;
    private final Clock clock;

    /**
     * 공간을 등록한다. 지역은 좌표로 판정하고, 관리자 검수 대기 상태로 저장한다.
     *
     * @throws BusinessException 서비스 지역 밖이면 SPACE_OUT_OF_SERVICE_AREA
     */
    @Transactional
    public ProviderSpaceResponse register(Long providerId, SpaceRequest request) {
        Member provider = memberRepository.findById(providerId)
                .filter(Member::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Region region = resolveRegion(request.latitude(), request.longitude());

        Space space = Space.register(provider, region, request.toDetails(), request.availableDates(), today());
        return responseMapper.providerSpace(spaceRepository.save(space));
    }

    Region resolveRegion(double latitude, double longitude) {
        return serviceRegionRepository.findRegionCodeContaining(latitude, longitude)
                .map(Region::valueOf)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_OUT_OF_SERVICE_AREA));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
