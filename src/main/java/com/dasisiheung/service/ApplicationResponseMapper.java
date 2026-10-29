package com.dasisiheung.service;

import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.entity.application.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 공간 대표 사진 주소가 필요한 신청 응답을 만든다. */
@Component
@RequiredArgsConstructor
public class ApplicationResponseMapper {

    private final SpaceResponseMapper spaceResponseMapper;

    public ApplicationResponse detail(Application application) {
        return ApplicationResponse.of(application, spaceResponseMapper.thumbnailUrl(application.getSpace()));
    }

    public ApplicationSummaryResponse summary(Application application) {
        return ApplicationSummaryResponse.of(application, spaceResponseMapper.thumbnailUrl(application.getSpace()));
    }
}
