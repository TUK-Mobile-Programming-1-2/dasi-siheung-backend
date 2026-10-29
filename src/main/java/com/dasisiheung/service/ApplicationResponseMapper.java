package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.PriceOfferResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.repository.PriceOfferRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 공간 대표 사진 주소 · 협의 이력이 필요한 신청 응답을 만든다. */
@Component
@RequiredArgsConstructor
public class ApplicationResponseMapper {

    private final SpaceResponseMapper spaceResponseMapper;
    private final PriceOfferRepository priceOfferRepository;

    public ApplicationResponse detail(Application application) {
        List<PriceOfferResponse> offers = application.getId() == null ? List.of()
                : priceOfferRepository.findByApplication_IdOrderByIdAsc(application.getId()).stream()
                        .map(PriceOfferResponse::from)
                        .toList();
        return ApplicationResponse.of(application, spaceResponseMapper.thumbnailUrl(application.getSpace()), offers);
    }

    public ApplicationSummaryResponse summary(Application application) {
        return ApplicationSummaryResponse.of(application, spaceResponseMapper.thumbnailUrl(application.getSpace()));
    }
}
