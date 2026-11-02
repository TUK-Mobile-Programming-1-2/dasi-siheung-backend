package com.dasisiheung.service;

import com.dasisiheung.dto.admin.AdminSpaceDetailResponse;
import com.dasisiheung.dto.admin.AdminSpaceDetailResponse.ProviderInfo;
import com.dasisiheung.dto.admin.AdminSpaceDetailResponse.ReferenceLink;
import com.dasisiheung.dto.admin.AdminSpaceSummaryResponse;
import com.dasisiheung.dto.admin.SpaceReviewRequest;
import com.dasisiheung.dto.admin.SpaceReviewResponse;
import com.dasisiheung.dto.admin.ThemeCorrectionRequest;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpaceReview;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.repository.SpaceReviewRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 공간 검수. 제출 증빙을 직접 확인하는 수동 검수이며, 부동산 공식 정보는 링크로만 보조한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpaceService {

    private static final List<ReferenceLink> OFFICIAL_LINKS = List.of(
            new ReferenceLink("건축물대장 열람 (정부24)", "https://www.gov.kr"),
            new ReferenceLink("등기사항증명서 열람 (인터넷등기소)", "https://www.iros.go.kr"),
            new ReferenceLink("부동산 공시가격 알리미", "https://www.realtyprice.kr"));

    private final SpaceRepository spaceRepository;
    private final SpaceReviewRepository spaceReviewRepository;
    private final MemberRepository memberRepository;
    private final SpaceResponseMapper responseMapper;
    private final Clock clock;

    /**
     * 공간 목록. 검수 대기는 오래 기다린 순(수정 시각 오름차순), 그 외는 최근 순.
     *
     * @param status null 이면 전체 현황
     */
    public PageResponse<AdminSpaceSummaryResponse> getSpaces(SpaceStatus status, PageQuery pageQuery) {
        Sort.Direction direction = status == SpaceStatus.PENDING_REVIEW ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = pageQuery.toPageable(Sort.by(direction, "updatedAt"));
        Page<Space> spaces = status == null
                ? spaceRepository.findAllBy(pageable)
                : spaceRepository.findByStatus(status, pageable);
        return PageResponse.from(spaces, AdminSpaceSummaryResponse::from);
    }

    public AdminSpaceDetailResponse getSpace(Long spaceId) {
        Space space = spaceRepository.getSpace(spaceId);
        return toDetail(space);
    }

    /**
     * 검수 결과를 반영한다. 승인하면 공개(PUBLISHED), 수정 요청·보류면 사유와 함께 제공자에게 보인다.
     */
    @Transactional
    public AdminSpaceDetailResponse review(Long adminId, Long spaceId, SpaceReviewRequest request) {
        Space space = spaceRepository.getSpace(spaceId);
        Member admin = memberRepository.getReferenceById(adminId);
        SpaceReview review = space.review(admin, request.decision(), request.reason(), request.toChecklist(),
                LocalDateTime.now(clock));
        spaceReviewRepository.save(review);
        spaceRepository.flush();
        return toDetail(space);
    }

    /** 상권 테마 보정. 추천 · 목적 검색에 쓰이며 다시 검수하지 않는다. */
    @Transactional
    public AdminSpaceDetailResponse correctTheme(Long spaceId, ThemeCorrectionRequest request) {
        Space space = spaceRepository.getSpace(spaceId);
        space.correctTheme(request.commercialTheme());
        spaceRepository.flush();
        return toDetail(space);
    }

    private AdminSpaceDetailResponse toDetail(Space space) {
        List<SpaceReviewResponse> reviews = spaceReviewRepository.findBySpace_IdOrderByCreatedAtDescIdDesc(space.getId())
                .stream().map(SpaceReviewResponse::from).toList();
        return new AdminSpaceDetailResponse(responseMapper.providerSpace(space), ProviderInfo.from(space.getProvider()),
                reviews, referenceLinks(space));
    }

    /** 공식 정보 사이트 + 등록 좌표의 지도 링크. 외부 조회가 안 돼도 제출 증빙만으로 검수할 수 있다. */
    private static List<ReferenceLink> referenceLinks(Space space) {
        String mapUrl = "https://map.kakao.com/link/map/%s,%s,%s".formatted(
                URLEncoder.encode(space.getTitle(), StandardCharsets.UTF_8), space.latitude(), space.longitude());
        return Stream.concat(OFFICIAL_LINKS.stream(),
                Stream.of(new ReferenceLink("등록 좌표 지도 (카카오맵)", mapUrl))).toList();
    }
}
