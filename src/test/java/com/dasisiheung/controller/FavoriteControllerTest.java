package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.FavoriteResponse;
import com.dasisiheung.service.FavoriteService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(FavoriteController.class)
class FavoriteControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Favorite";

    @MockitoBean
    private FavoriteService favoriteService;

    @Test
    @DisplayName("찜하기")
    void add() throws Exception {
        given(favoriteService.add(USER_ID, 10L)).willReturn(new FavoriteResponse(10L, true, 13));

        mockMvc.perform(put("/api/v1/spaces/{spaceId}/favorite", 10L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andDo(document("favorite-add",
                        resource(TAG, "찜하기", "이미 찜했으면 그대로 둡니다(여러 번 보내도 같은 결과). "
                                + "공개 중인 공간만 찜할 수 있습니다 (아니면 404 SPACE_NOT_FOUND)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(resultFields()))));
    }

    @Test
    @DisplayName("찜 해제")
    void remove() throws Exception {
        given(favoriteService.remove(USER_ID, 10L)).willReturn(new FavoriteResponse(10L, false, 12));

        mockMvc.perform(delete("/api/v1/spaces/{spaceId}/favorite", 10L).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false))
                .andDo(document("favorite-remove",
                        resource(TAG, "찜 해제", "찜하지 않았어도 성공합니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(resultFields()))));
    }

    @Test
    @DisplayName("내 찜 목록")
    void getMyFavorites() throws Exception {
        given(favoriteService.getMyFavorites(eq(USER_ID), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(SpaceControllerTest.spaceSummary()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/members/me/favorites").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("favorite-my-list",
                        resource(TAG, "내 찜 목록", "공개 중인 공간만, 최근 찜한 순입니다."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(SpaceControllerTest.summaryFields("data.content[].")))));
    }

    @Test
    @DisplayName("공간 제공자 · 비로그인은 찜할 수 없다")
    void forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/spaces/{spaceId}/favorite", 10L).header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/spaces/{spaceId}/favorite", 10L))
                .andExpect(status().isUnauthorized());
    }

    private static FieldDescriptor[] resultFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.spaceId").type(JsonFieldType.NUMBER).description("공간 ID"),
                fieldWithPath("data.favorited").type(JsonFieldType.BOOLEAN).description("지금 찜한 상태인지"),
                fieldWithPath("data.favoriteCount").type(JsonFieldType.NUMBER).description("공간의 찜 수")};
    }
}
