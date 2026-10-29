package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.SpaceQueryService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceDocumentation;
import com.dasisiheung.support.SpaceFixtures;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(SpaceController.class)
class SpaceControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Space";

    @MockitoBean
    private SpaceQueryService spaceQueryService;

    @Test
    @DisplayName("공간 상세: 로그인 없이 조회한다")
    void getSpace() throws Exception {
        given(spaceQueryService.getSpace(10L)).willReturn(spaceDetail());

        mockMvc.perform(get("/api/v1/spaces/{spaceId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider.name").value("제공자"))
                .andExpect(jsonPath("$.data.photos[0].sortOrder").value(0))
                .andDo(document("space-detail",
                        resource(TAG, "공간 상세", "공개 중인 공간만 조회됩니다. 인증 없이 호출할 수 있습니다."),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(SpaceDocumentation.spaceDetailFields("data.")
                                .toArray(FieldDescriptor[]::new)))));
    }

    @Test
    @DisplayName("공간 상세: 공개 중이 아니면 404 SPACE_NOT_FOUND")
    void notPublished() throws Exception {
        given(spaceQueryService.getSpace(99L)).willThrow(new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/spaces/{spaceId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPACE_NOT_FOUND"))
                .andDo(document("space-detail-not-found",
                        resource(TAG, "공간 상세", "없거나 공개 중이 아닌 공간 (검수 대기 · 일시 중단 · 숨김)"),
                        responseFields(error())));
    }

    static SpaceDetailResponse spaceDetail() {
        return SpaceDetailResponse.of(
                SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                        Set.of(ProviderSpaceControllerTest.TOMORROW), FixedClockConfig.FIXED_TODAY),
                List.of(ProviderSpaceControllerTest.TOMORROW), ProviderSpaceControllerTest.photos());
    }
}
