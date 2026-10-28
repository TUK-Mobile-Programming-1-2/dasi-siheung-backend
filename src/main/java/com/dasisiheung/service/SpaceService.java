package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.ProviderSpaceSummaryResponse;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.ServiceRegionRepository;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /** @param status null 이면 전체 */
    public PageResponse<ProviderSpaceSummaryResponse> getMySpaces(Long providerId, SpaceStatus status, PageQuery pageQuery) {
        Pageable pageable = pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Space> spaces = status == null
                ? spaceRepository.findByProvider_Id(providerId, pageable)
                : spaceRepository.findByProvider_IdAndStatus(providerId, status, pageable);
        return PageResponse.from(spaces, responseMapper::providerSpaceSummary);
    }

    public ProviderSpaceResponse getMySpace(Long providerId, Long spaceId) {
        return responseMapper.providerSpace(getOwnedSpace(providerId, spaceId));
    }

    /**
     * 공간 정보 수정. 공개·일시 중단 중에 검수 항목이 바뀌면 다시 검수 대기가 된다.
     * availableDates 를 보내지 않으면 캘린더는 그대로 둔다.
     */
    @Transactional
    public ProviderSpaceResponse update(Long providerId, Long spaceId, SpaceRequest request) {
        Space space = getOwnedSpace(providerId, spaceId);
        Region region = resolveRegion(request.latitude(), request.longitude());
        space.update(region, request.toDetails(), request.availableDates(), today());
        spaceRepository.flush();
        return responseMapper.providerSpace(space);
    }

    @Transactional
    public ProviderSpaceResponse changeAvailableDates(Long providerId, Long spaceId, Set<LocalDate> dates) {
        Space space = getOwnedSpace(providerId, spaceId);
        space.validateEditable();
        space.changeAvailableDates(dates, today());
        return responseMapper.providerSpace(space);
    }

    @Transactional
    public ProviderSpaceResponse pause(Long providerId, Long spaceId) {
        Space space = getOwnedSpace(providerId, spaceId);
        space.pause();
        return responseMapper.providerSpace(space);
    }

    @Transactional
    public ProviderSpaceResponse resume(Long providerId, Long spaceId) {
        Space space = getOwnedSpace(providerId, spaceId);
        space.resume();
        return responseMapper.providerSpace(space);
    }

    @Transactional
    public ProviderSpaceResponse resubmit(Long providerId, Long spaceId) {
        Space space = getOwnedSpace(providerId, spaceId);
        space.resubmit();
        return responseMapper.providerSpace(space);
    }

    private Space getOwnedSpace(Long providerId, Long spaceId) {
        Space space = spaceRepository.getSpace(spaceId);
        space.validateOwner(providerId);
        return space;
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
