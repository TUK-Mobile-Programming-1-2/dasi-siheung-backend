package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.admin.AdminSpaceDetailResponse;
import com.dasisiheung.dto.admin.AdminSpaceDetailResponse.ProviderInfo;
import com.dasisiheung.dto.admin.AdminSpaceDetailResponse.ReferenceLink;
import com.dasisiheung.dto.admin.AdminSpaceSummaryResponse;
import com.dasisiheung.dto.admin.SpaceReviewRequest;
import com.dasisiheung.dto.admin.SpaceReviewResponse;
import com.dasisiheung.dto.admin.ThemeCorrectionRequest;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.ReviewDecision;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.AdminSpaceService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceDocumentation;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(AdminSpaceController.class)
class AdminSpaceControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Admin Space";

    @MockitoBean
    private AdminSpaceService adminSpaceService;

    @Test
    @DisplayName("검수 대기 목록")
    void getPendingSpaces() throws Exception {
        AdminSpaceSummaryResponse summary = new AdminSpaceSummaryResponse(10L, "정왕역 앞 1층 상가", Region.JEONGWANG,
                "경기도 시흥시 마유로 418", SpaceStatus.PENDING_REVIEW, "김제공", 1, 2,
                FixedClockConfig.FIXED_LOCAL_NOW, FixedClockConfig.FIXED_LOCAL_NOW);
        given(adminSpaceService.getSpaces(eq(SpaceStatus.PENDING_REVIEW), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/admin/spaces").param("status", "PENDING_REVIEW")
                        .header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].proofCount").value(1))
                .andDo(document("admin-space-list",
                        resource(TAG, "공간 목록 (검수 대기 · 전체 현황)",
                                "status=PENDING_REVIEW 면 오래 기다린 순, 생략하면 전체 공간을 최근 순으로 보여줍니다."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("status").optional().description("공개 상태 필터 (선택지 spaceStatuses)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("공간 ID"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("공간 이름"),
                                fieldWithPath("data.content[].region").type(JsonFieldType.STRING).description("서비스 지역"),
                                fieldWithPath("data.content[].address").type(JsonFieldType.STRING).description("주소"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING).description("공개 상태"),
                                fieldWithPath("data.content[].providerName").type(JsonFieldType.STRING).description("공간 제공자 이름"),
                                fieldWithPath("data.content[].proofCount").type(JsonFieldType.NUMBER).description("증빙 수 (0 이면 승인 불가)"),
                                fieldWithPath("data.content[].photoCount").type(JsonFieldType.NUMBER).description("사진 수"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                                fieldWithPath("data.content[].updatedAt").type(JsonFieldType.STRING).description("마지막 수정 시각")))));
    }

    @Test
    @DisplayName("검수 상세: 증빙 임시 주소 · 제공자 · 이력 · 공식 정보 링크")
    void getSpace() throws Exception {
        given(adminSpaceService.getSpace(10L)).willReturn(detail());

        mockMvc.perform(get("/api/v1/admin/spaces/{spaceId}", 10L).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider.name").value("김제공"))
                .andDo(document("admin-space-detail",
                        resource(TAG, "검수 상세", "공식 정보 링크는 보조 자료입니다. 외부 조회가 안 돼도 제출 증빙만으로 검수합니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        responseFields(success(detailFields()))));
    }

    @Test
    @DisplayName("검수 결과 등록 (수정 요청 예시)")
    void review() throws Exception {
        given(adminSpaceService.review(eq(ADMIN_ID), eq(10L), any(SpaceReviewRequest.class))).willReturn(detail());

        mockMvc.perform(post("/api/v1/admin/spaces/{spaceId}/reviews", 10L)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SpaceReviewRequest(ReviewDecision.REQUEST_REVISION, "등기사항증명서와 호실이 다릅니다.",
                                new SpaceReviewRequest.Checklist(true, true, false, true, true)))))
                .andExpect(status().isOk())
                .andDo(document("admin-space-review",
                        resource(TAG, "검수 결과 등록", "검수 대기 공간만 가능(아니면 409 INVALID_SPACE_STATUS). "
                                + "공개 승인은 증빙 1개 이상(400 SPACE_PROOF_REQUIRED) + 5개 항목 모두 true(400 REVIEW_CHECKLIST_INCOMPLETE), "
                                + "수정 요청·공개 보류는 사유 필수(400 REVIEW_REASON_REQUIRED). 사유는 제공자에게 그대로 보입니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestFields(
                                fieldWithPath("decision").type(JsonFieldType.STRING).description("APPROVE 공개 승인 | REQUEST_REVISION 수정 요청 | HOLD 공개 보류"),
                                fieldWithPath("reason").type(JsonFieldType.STRING).optional().description("사유 (수정 요청·보류 필수)"),
                                fieldWithPath("checklist.authorityVerified").type(JsonFieldType.BOOLEAN).description("제공 권한 확인"),
                                fieldWithPath("checklist.addressMatched").type(JsonFieldType.BOOLEAN).description("주소 일치"),
                                fieldWithPath("checklist.buildingUnitMatched").type(JsonFieldType.BOOLEAN).description("건물·호실 일치"),
                                fieldWithPath("checklist.areaMatched").type(JsonFieldType.BOOLEAN).description("면적 일치"),
                                fieldWithPath("checklist.usageMatched").type(JsonFieldType.BOOLEAN).description("용도 일치")),
                        responseFields(success(detailFields()))));
    }

    @Test
    @DisplayName("검수 결과 등록: 항목을 모두 확인하지 않고 승인하면 400 REVIEW_CHECKLIST_INCOMPLETE")
    void approveWithIncompleteChecklist() throws Exception {
        given(adminSpaceService.review(eq(ADMIN_ID), eq(10L), any(SpaceReviewRequest.class)))
                .willThrow(new BusinessException(ErrorCode.REVIEW_CHECKLIST_INCOMPLETE));

        mockMvc.perform(post("/api/v1/admin/spaces/{spaceId}/reviews", 10L)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SpaceReviewRequest(ReviewDecision.APPROVE, null,
                                new SpaceReviewRequest.Checklist(true, true, true, false, true)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REVIEW_CHECKLIST_INCOMPLETE"))
                .andDo(document("admin-space-review-incomplete",
                        resource(TAG, "검수 결과 등록", "검수 항목 미확인 승인 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("상권 테마 보정")
    void correctTheme() throws Exception {
        given(adminSpaceService.correctTheme(eq(10L), any(ThemeCorrectionRequest.class))).willReturn(detail());

        mockMvc.perform(put("/api/v1/admin/spaces/{spaceId}/theme", 10L)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ThemeCorrectionRequest(CommercialTheme.CAFE_STREET))))
                .andExpect(status().isOk())
                .andDo(document("admin-space-theme",
                        resource(TAG, "상권 테마 보정", "추천 · 목적 검색 품질을 위한 보정이라 다시 검수하지 않습니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("spaceId").description("공간 ID")),
                        requestFields(fieldWithPath("commercialTheme").type(JsonFieldType.STRING).optional()
                                .description("상권 테마 (선택지 commercialThemes, null 이면 비움)")),
                        responseFields(success(detailFields()))));
    }

    @Test
    @DisplayName("관리자가 아니면 403 FORBIDDEN")
    void nonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/spaces").header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private static AdminSpaceDetailResponse detail() {
        SpaceReviewResponse history = new SpaceReviewResponse(1L, ReviewDecision.REQUEST_REVISION,
                "등기사항증명서와 호실이 다릅니다.", new SpaceReviewRequest.Checklist(true, true, false, true, true),
                "관리자", FixedClockConfig.FIXED_LOCAL_NOW);
        return new AdminSpaceDetailResponse(ProviderSpaceControllerTest.providerSpaceWithFiles(),
                new ProviderInfo(PROVIDER_ID, "김제공", "provider@example.com", "01022223333"),
                List.of(history),
                List.of(new ReferenceLink("건축물대장 열람 (정부24)", "https://www.gov.kr")));
    }

    private static FieldDescriptor[] detailFields() {
        List<FieldDescriptor> fields = new ArrayList<>(SpaceDocumentation.providerSpaceFields("data.space."));
        fields.addAll(SpaceDocumentation.photoFields("data.space.photos[]."));
        fields.addAll(SpaceDocumentation.proofFields("data.space.proofs[]."));
        fields.addAll(List.of(
                fieldWithPath("data.provider.id").type(JsonFieldType.NUMBER).description("제공자 회원 ID"),
                fieldWithPath("data.provider.name").type(JsonFieldType.STRING).description("제공자 이름"),
                fieldWithPath("data.provider.email").type(JsonFieldType.STRING).description("제공자 이메일"),
                fieldWithPath("data.provider.phone").type(JsonFieldType.STRING).optional().description("제공자 휴대폰 번호"),
                fieldWithPath("data.reviews[].id").type(JsonFieldType.NUMBER).description("검수 ID"),
                fieldWithPath("data.reviews[].decision").type(JsonFieldType.STRING).description("검수 결과"),
                fieldWithPath("data.reviews[].reason").type(JsonFieldType.STRING).optional().description("사유"),
                fieldWithPath("data.reviews[].checklist.authorityVerified").type(JsonFieldType.BOOLEAN).description("제공 권한 확인"),
                fieldWithPath("data.reviews[].checklist.addressMatched").type(JsonFieldType.BOOLEAN).description("주소 일치"),
                fieldWithPath("data.reviews[].checklist.buildingUnitMatched").type(JsonFieldType.BOOLEAN).description("건물·호실 일치"),
                fieldWithPath("data.reviews[].checklist.areaMatched").type(JsonFieldType.BOOLEAN).description("면적 일치"),
                fieldWithPath("data.reviews[].checklist.usageMatched").type(JsonFieldType.BOOLEAN).description("용도 일치"),
                fieldWithPath("data.reviews[].adminName").type(JsonFieldType.STRING).description("검수한 관리자"),
                fieldWithPath("data.reviews[].reviewedAt").type(JsonFieldType.STRING).description("검수 시각"),
                fieldWithPath("data.referenceLinks[].name").type(JsonFieldType.STRING).description("공식 정보 사이트 이름"),
                fieldWithPath("data.referenceLinks[].url").type(JsonFieldType.STRING).description("주소")));
        return fields.toArray(FieldDescriptor[]::new);
    }
}
