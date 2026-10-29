package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.admin.DashboardResponse;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.service.AdminDashboardService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(AdminDashboardController.class)
class AdminDashboardControllerTest extends ApiDocsTestSupport {

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("대시보드 수치")
    void getDashboard() throws Exception {
        given(adminDashboardService.getDashboard()).willReturn(dashboard());

        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingReviewCount").value(2))
                .andExpect(jsonPath("$.data.spacesByStatus.PUBLISHED").value(4))
                .andDo(document("admin-dashboard",
                        resource("Admin Dashboard", "대시보드 수치", "상태·지역·역할별 개수는 0 인 항목도 모두 내려줍니다."),
                        requestHeaders(authorization()),
                        responseFields(success(
                                fieldWithPath("data.pendingReviewCount").type(JsonFieldType.NUMBER).description("검수 대기 공간 수"),
                                subsectionWithPath("data.spacesByStatus").type(JsonFieldType.OBJECT)
                                        .description("상태별 공간 수 { PENDING_REVIEW: 2, PUBLISHED: 4, ... }"),
                                subsectionWithPath("data.publishedSpacesByRegion").type(JsonFieldType.OBJECT)
                                        .description("지역별 공개 공간 수 { JEONGWANG: 3, ... }"),
                                subsectionWithPath("data.membersByRole").type(JsonFieldType.OBJECT)
                                        .description("역할별 회원 수 (탈퇴 제외) { USER: 10, ... }"),
                                fieldWithPath("data.recentSpaces[].id").type(JsonFieldType.NUMBER).description("공간 ID"),
                                fieldWithPath("data.recentSpaces[].title").type(JsonFieldType.STRING).description("공간 이름"),
                                fieldWithPath("data.recentSpaces[].region").type(JsonFieldType.STRING).description("지역"),
                                fieldWithPath("data.recentSpaces[].status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.recentSpaces[].createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                                fieldWithPath("data.generatedAt").type(JsonFieldType.STRING).description("집계 시각")))));
    }

    static DashboardResponse dashboard() {
        Map<SpaceStatus, Long> byStatus = new LinkedHashMap<>();
        for (SpaceStatus status : SpaceStatus.values()) {
            byStatus.put(status, 0L);
        }
        byStatus.put(SpaceStatus.PENDING_REVIEW, 2L);
        byStatus.put(SpaceStatus.PUBLISHED, 4L);
        Map<Region, Long> byRegion = new LinkedHashMap<>(Map.of(Region.JEONGWANG, 3L, Region.GEOBUKSEOM, 0L, Region.BAEGOT, 1L));
        Map<Role, Long> byRole = new LinkedHashMap<>(Map.of(Role.USER, 10L, Role.PROVIDER, 5L, Role.ADMIN, 1L));
        return new DashboardResponse(2, byStatus, byRegion, byRole,
                List.of(new DashboardResponse.RecentSpace(10L, "정왕역 앞 1층 상가", Region.JEONGWANG,
                        SpaceStatus.PENDING_REVIEW, FixedClockConfig.FIXED_LOCAL_NOW)),
                FixedClockConfig.FIXED_LOCAL_NOW);
    }
}
