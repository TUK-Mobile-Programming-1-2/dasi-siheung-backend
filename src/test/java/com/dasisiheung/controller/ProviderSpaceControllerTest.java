package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.SpaceService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceDocumentation;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(ProviderSpaceController.class)
class ProviderSpaceControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Provider Space";
    static final LocalDate TOMORROW = FixedClockConfig.FIXED_TODAY.plusDays(1);

    @MockitoBean
    private SpaceService spaceService;

    @Test
    @DisplayName("공간 등록: 201 과 검수 대기 상태의 내 공간 상세를 반환한다")
    void register() throws Exception {
        SpaceRequest request = SpaceFixtures.request(Set.of(TOMORROW, TOMORROW.plusDays(1)));
        given(spaceService.register(eq(PROVIDER_ID), any(SpaceRequest.class))).willReturn(providerSpace());

        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.region").value("JEONGWANG"))
                .andDo(document("provider-space-register",
                        resource(TAG, "공간 등록", "지역은 좌표로 판정하며 정왕동·거북섬·배곧 밖이면 400 SPACE_OUT_OF_SERVICE_AREA 입니다. "
                                + "등록하면 검수 대기(PENDING_REVIEW) 상태가 되고, 사진·증빙은 등록 후 따로 올립니다."),
                        requestHeaders(authorization()),
                        requestFields(SpaceDocumentation.spaceRequestFields()),
                        responseFields(success(providerSpaceFields()))));
    }

    @Test
    @DisplayName("공간 등록: 서비스 지역 밖이면 400 SPACE_OUT_OF_SERVICE_AREA")
    void registerOutOfServiceArea() throws Exception {
        given(spaceService.register(eq(PROVIDER_ID), any(SpaceRequest.class)))
                .willThrow(new BusinessException(ErrorCode.SPACE_OUT_OF_SERVICE_AREA));

        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(SpaceFixtures.request(SpaceFixtures.SEOUL_LAT, SpaceFixtures.SEOUL_LNG, Set.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SPACE_OUT_OF_SERVICE_AREA"))
                .andDo(document("provider-space-register-out-of-area",
                        resource(TAG, "공간 등록", "서비스 지역 밖 좌표 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("공간 등록: 이용자(USER)는 403 FORBIDDEN")
    void registerByUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(SpaceFixtures.request(Set.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("공간 등록: 허용 활동이 비어 있으면 400")
    void registerWithoutActivities() throws Exception {
        mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "상가", "description": "소개", "address": "경기도 시흥시 마유로 418",
                                 "latitude": 37.3517, "longitude": 126.7428, "areaSqm": 33.06,
                                 "buildingUse": "NEIGHBORHOOD_1", "dailyPrice": 50000, "activityTypes": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("activityTypes"));
    }

    static ProviderSpaceResponse providerSpace() {
        return ProviderSpaceResponse.from(SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                Set.of(TOMORROW, TOMORROW.plusDays(1)), FixedClockConfig.FIXED_TODAY));
    }

    static FieldDescriptor[] providerSpaceFields() {
        List<FieldDescriptor> fields = new ArrayList<>(SpaceDocumentation.providerSpaceFields("data."));
        return fields.toArray(FieldDescriptor[]::new);
    }
}
