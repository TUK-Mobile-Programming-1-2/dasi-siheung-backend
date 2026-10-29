package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.dto.space.SpaceMapRequest;
import com.dasisiheung.dto.space.SpaceMapResponse;
import com.dasisiheung.dto.space.SpaceSearchRequest;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.repository.SpaceSpecifications;
import com.dasisiheung.repository.projection.SpaceMarker;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이용자의 공간 둘러보기 (로그인 없이도 가능). 공개(PUBLISHED) 공간만 보인다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceQueryService {

    /** 기간 검색은 오늘부터 이 날수 안에서만 한다. */
    public static final int MAX_SEARCH_PERIOD_DAYS = 90;
    /** 지도 한 번에 내려주는 마커 수 상한. 넘으면 truncated = true. */
    public static final int MAX_MAP_MARKERS = 300;

    private final SpaceRepository spaceRepository;
    private final ApplicationRepository applicationRepository;
    private final SpaceResponseMapper responseMapper;
    private final Clock clock;

    /**
     * @throws BusinessException 없거나 공개 중이 아니면 SPACE_NOT_FOUND (검수 대기·숨김 공간의 존재를 드러내지 않는다)
     */
    public SpaceDetailResponse getSpace(Long spaceId) {
        Space space = getPublishedSpace(spaceId);
        LocalDate today = LocalDate.now(clock);
        return responseMapper.spaceDetail(space, today, bookedDates(spaceId, today));
    }

    /** 오늘 이후 확정 예약이 있는 날짜. */
    private Set<LocalDate> bookedDates(Long spaceId, LocalDate today) {
        return applicationRepository.findApprovedEndingFrom(spaceId, today).stream()
                .flatMap(application -> application.period().dates())
                .collect(Collectors.toSet());
    }

    /**
     * 조건에 맞는 공개 공간을 찾는다. 기간을 주면 그 기간에 확정 예약이 없는 공간만 나온다.
     *
     * @throws BusinessException 기간이 오늘 이전이거나 90일을 넘으면 INVALID_SEARCH_PERIOD
     */
    public PageResponse<SpaceSummaryResponse> search(SpaceSearchRequest request, PageQuery pageQuery) {
        if (request.hasPeriod()) {
            validatePeriod(request.startDate(), request.endDate());
        }
        Page<Space> spaces = spaceRepository.findAll(toSpecification(request),
                pageQuery.toPageable(request.sort().toSort()));
        return PageResponse.from(spaces, responseMapper::spaceSummary);
    }

    /** 지도 화면 영역 안의 공개 공간 마커. 상한보다 하나 더 읽어 잘렸는지 판단한다. */
    public SpaceMapResponse getMarkers(SpaceMapRequest request) {
        String activityType = request.activityType() == null ? "" : request.activityType().name();
        int maxPrice = request.maxPrice() == null ? Integer.MAX_VALUE : request.maxPrice();
        List<SpaceMarker> found = spaceRepository.findMarkersWithin(request.swLat(), request.swLng(),
                request.neLat(), request.neLng(), activityType, maxPrice, MAX_MAP_MARKERS + 1);

        boolean truncated = found.size() > MAX_MAP_MARKERS;
        List<SpaceMapResponse.Marker> markers = found.stream()
                .limit(MAX_MAP_MARKERS)
                .map(SpaceQueryService::toMarker)
                .toList();
        return new SpaceMapResponse(markers, truncated);
    }

    private static SpaceMapResponse.Marker toMarker(SpaceMarker marker) {
        return new SpaceMapResponse.Marker(marker.getId(), marker.getTitle(), Region.valueOf(marker.getRegion()),
                marker.getDailyPrice(), marker.getLatitude(), marker.getLongitude());
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(clock);
        boolean outOfWindow = startDate.isBefore(today) || endDate.isAfter(today.plusDays(MAX_SEARCH_PERIOD_DAYS));
        if (outOfWindow) {
            throw new BusinessException(ErrorCode.INVALID_SEARCH_PERIOD);
        }
    }

    static Specification<Space> toSpecification(SpaceSearchRequest request) {
        List<Specification<Space>> conditions = new ArrayList<>();
        conditions.add(SpaceSpecifications.hasStatus(SpaceStatus.PUBLISHED));
        if (request.region() != null) {
            conditions.add(SpaceSpecifications.inRegion(request.region()));
        }
        if (request.activityType() != null) {
            conditions.add(SpaceSpecifications.allowsActivity(request.activityType()));
        }
        if (request.purpose() != null) {
            conditions.add(SpaceSpecifications.themeIn(request.purpose().getSuitableThemes()));
        }
        if (request.hasPeriod()) {
            conditions.add(SpaceSpecifications.availableThroughout(request.startDate(), request.endDate()));
            conditions.add(SpaceSpecifications.notBookedBetween(request.startDate(), request.endDate()));
        }
        if (request.minPrice() != null) {
            conditions.add(SpaceSpecifications.priceAtLeast(request.minPrice()));
        }
        if (request.maxPrice() != null) {
            conditions.add(SpaceSpecifications.priceAtMost(request.maxPrice()));
        }
        if (!request.facilities().isEmpty()) {
            conditions.add(SpaceSpecifications.hasAllFacilities(request.facilities()));
        }
        if (request.keyword() != null) {
            conditions.add(SpaceSpecifications.containsKeyword(request.keyword()));
        }
        return Specification.allOf(conditions);
    }

    private Space getPublishedSpace(Long spaceId) {
        return spaceRepository.findByIdAndStatus(spaceId, SpaceStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPACE_NOT_FOUND));
    }
}
