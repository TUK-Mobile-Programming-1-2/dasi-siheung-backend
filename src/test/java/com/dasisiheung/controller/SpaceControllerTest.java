package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.dto.space.SpaceSort;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Facility;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.SpaceQueryService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceDocumentation;
import com.dasisiheung.support.SpaceFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(SpaceController.class)
class SpaceControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Space";

    @MockitoBean
    private SpaceQueryService spaceQueryService;

    @Test
    @DisplayName("공간 검색: 조건을 쿼리 파라미터로 받아 공개 공간 카드를 페이지로 준다")
    void search() throws Exception {
        given(spaceQueryService.search(argThat(request -> request.region() == Region.JEONGWANG
                        && request.facilities().contains(Facility.WIFI) && request.sort() == SpaceSort.PRICE_ASC),
                any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(spaceSummary()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/spaces")
                        .param("region", "JEONGWANG").param("activityType", "POPUP_STORE").param("purpose", "SALES")
                        .param("startDate", "2026-10-24").param("endDate", "2026-10-25")
                        .param("minPrice", "30000").param("maxPrice", "80000")
                        .param("facilities", "WIFI", "ELECTRICITY").param("keyword", "정왕역")
                        .param("sort", "PRICE_ASC").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").exists())
                .andDo(document("space-search",
                        resource(TAG, "공간 검색", "모든 조건은 선택이며 함께 보내면 모두 만족하는 공개 공간만 나옵니다. "
                                + "기간은 오늘부터 90일 안(아니면 400 INVALID_SEARCH_PERIOD), 시작일·종료일은 함께 보냅니다."),
                        queryParameters(
                                parameterWithName("region").optional().description("서비스 지역 (선택지 regions)"),
                                parameterWithName("activityType").optional().description("하고 싶은 활동 (선택지 activityTypes)"),
                                parameterWithName("purpose").optional().description("이용 목적 — 맞는 상권 테마의 공간만 (선택지 usePurposes)"),
                                parameterWithName("startDate").optional().description("이용 시작일 yyyy-MM-dd"),
                                parameterWithName("endDate").optional().description("이용 종료일 yyyy-MM-dd — 기간의 모든 날짜가 비어 있는 공간만"),
                                parameterWithName("minPrice").optional().description("1일 이용료 하한(원)"),
                                parameterWithName("maxPrice").optional().description("1일 이용료 상한(원) — 예산"),
                                parameterWithName("facilities").optional().description("꼭 있어야 하는 시설, 여러 번 보낼 수 있음 (모두 갖춘 공간만)"),
                                parameterWithName("keyword").optional().description("공간 이름 · 주소 검색어 (최대 50자)"),
                                parameterWithName("sort").optional().description("정렬 (선택지 spaceSorts, 기본 LATEST)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(summaryFields("data.content[].")))));
    }

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

    static SpaceSummaryResponse spaceSummary() {
        return new SpaceSummaryResponse(10L, "정왕역 앞 1층 상가", Region.JEONGWANG, "경기도 시흥시 마유로 418",
                ProviderSpaceControllerTest.photos().get(0).url(), 50_000, new BigDecimal("33.06"),
                CommercialTheme.FOOD_ALLEY, List.of(ActivityType.POPUP_STORE, ActivityType.EXHIBITION),
                37.3517, 126.7428);
    }

    static FieldDescriptor[] summaryFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath(prefix + "title").type(JsonFieldType.STRING).description("공간 이름"),
                fieldWithPath(prefix + "region").type(JsonFieldType.STRING).description("서비스 지역"),
                fieldWithPath(prefix + "address").type(JsonFieldType.STRING).description("도로명 주소"),
                fieldWithPath(prefix + "thumbnailUrl").type(JsonFieldType.STRING).optional().description("대표 사진 주소 (없으면 null)"),
                fieldWithPath(prefix + "dailyPrice").type(JsonFieldType.NUMBER).description("1일 이용료(원)"),
                fieldWithPath(prefix + "areaSqm").type(JsonFieldType.NUMBER).description("전용 면적(㎡)"),
                fieldWithPath(prefix + "commercialTheme").type(JsonFieldType.STRING).optional().description("상권 테마"),
                fieldWithPath(prefix + "activityTypes").type(JsonFieldType.ARRAY).description("허용 활동"),
                fieldWithPath(prefix + "latitude").type(JsonFieldType.NUMBER).description("위도"),
                fieldWithPath(prefix + "longitude").type(JsonFieldType.NUMBER).description("경도")};
    }

    static SpaceDetailResponse spaceDetail() {
        return SpaceDetailResponse.of(
                SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                        Set.of(ProviderSpaceControllerTest.TOMORROW), FixedClockConfig.FIXED_TODAY),
                List.of(ProviderSpaceControllerTest.TOMORROW), ProviderSpaceControllerTest.photos());
    }
}
