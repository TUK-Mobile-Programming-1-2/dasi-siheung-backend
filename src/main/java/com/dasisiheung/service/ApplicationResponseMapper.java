package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.PriceOfferResponse;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.repository.ConversationRepository;
import com.dasisiheung.repository.PriceOfferRepository;
import com.dasisiheung.repository.projection.ConversationLink;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** 공간 대표 사진 주소 · 협의 이력 · 연결된 1:1 대화가 필요한 신청 응답을 만든다. */
@Component
@RequiredArgsConstructor
public class ApplicationResponseMapper {

    private final SpaceResponseMapper spaceResponseMapper;
    private final PriceOfferRepository priceOfferRepository;
    private final ConversationRepository conversationRepository;

    public ApplicationResponse detail(Application application) {
        List<PriceOfferResponse> offers = application.getId() == null ? List.of()
                : priceOfferRepository.findByApplication_IdOrderByIdAsc(application.getId()).stream()
                        .map(PriceOfferResponse::from)
                        .toList();
        Long conversationId = conversationRepository
                .findIdBySpaceIdAndUserId(application.getSpace().getId(), application.getApplicant().getId())
                .orElse(null);
        return ApplicationResponse.of(application, spaceResponseMapper.thumbnailUrl(application.getSpace()), offers,
                conversationId);
    }

    /** 목록. 연결된 대화는 페이지 단위로 한 번에 찾는다. (항목마다 조회하지 않음) */
    public PageResponse<ApplicationSummaryResponse> summaries(Page<Application> applications) {
        Map<ConversationKey, Long> conversationIds = conversationIds(applications.getContent());
        return PageResponse.from(applications, application -> ApplicationSummaryResponse.of(application,
                spaceResponseMapper.thumbnailUrl(application.getSpace()),
                conversationIds.get(ConversationKey.of(application))));
    }

    private Map<ConversationKey, Long> conversationIds(List<Application> applications) {
        if (applications.isEmpty()) {
            return Map.of();
        }
        Set<Long> spaceIds = applications.stream().map(a -> a.getSpace().getId()).collect(Collectors.toSet());
        Set<Long> userIds = applications.stream().map(a -> a.getApplicant().getId()).collect(Collectors.toSet());
        return conversationRepository.findLinks(spaceIds, userIds).stream()
                .collect(Collectors.toMap(link -> new ConversationKey(link.spaceId(), link.userId()),
                        ConversationLink::conversationId));
    }

    /** 대화는 공간 · 이용자 한 쌍에 하나다. */
    private record ConversationKey(Long spaceId, Long userId) {

        static ConversationKey of(Application application) {
            return new ConversationKey(application.getSpace().getId(), application.getApplicant().getId());
        }
    }
}
