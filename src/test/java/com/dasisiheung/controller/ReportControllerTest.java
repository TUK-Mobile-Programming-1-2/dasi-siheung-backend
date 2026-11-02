package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.report.AdminReportResponse;
import com.dasisiheung.dto.report.MemberSuspensionResponse;
import com.dasisiheung.dto.report.ReportRequest;
import com.dasisiheung.dto.report.ReportResponse;
import com.dasisiheung.dto.report.ResolveReportRequest;
import com.dasisiheung.dto.report.SuspendRequest;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.report.ReportAction;
import com.dasisiheung.entity.report.ReportReason;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.entity.report.ReportTargetType;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.AdminReportService;
import com.dasisiheung.service.ReportService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest({ReportController.class, AdminReportController.class})
class ReportControllerTest extends ApiDocsTestSupport {

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private AdminReportService adminReportService;

    @Test
    @DisplayName("신고하기")
    void report() throws Exception {
        given(reportService.report(eq(USER_ID), any(ReportRequest.class))).willReturn(new ReportResponse(1L,
                ReportTargetType.SPACE, 10L, ReportReason.FALSE_INFORMATION, "사진과 실제 공간이 전혀 다릅니다.",
                ReportStatus.PENDING, FixedClockConfig.FIXED_LOCAL_NOW));

        mockMvc.perform(post("/api/v1/reports").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReportRequest(ReportTargetType.SPACE, 10L, ReportReason.FALSE_INFORMATION,
                                "사진과 실제 공간이 전혀 다릅니다."))))
                .andExpect(status().isCreated())
                .andDo(document("report-create",
                        resource("Report", "신고하기", "로그인 회원 누구나. 대상이 없으면 404 REPORT_TARGET_NOT_FOUND, "
                                + "자기 자신 · 자기 글은 400 CANNOT_REPORT_SELF, 처리 대기 중 같은 신고는 409 DUPLICATE_REPORT."),
                        requestHeaders(authorization()),
                        requestFields(
                                fieldWithPath("targetType").type(JsonFieldType.STRING).description("SPACE | POST | COMMENT | MEMBER"),
                                fieldWithPath("targetId").type(JsonFieldType.NUMBER).description("대상 ID"),
                                fieldWithPath("reason").type(JsonFieldType.STRING).description("사유 (선택지 reportReasons)"),
                                fieldWithPath("detail").type(JsonFieldType.STRING).optional().description("자세한 내용 (선택)")),
                        responseFields(success(
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("신고 ID"),
                                fieldWithPath("data.targetType").type(JsonFieldType.STRING).description("대상 종류"),
                                fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("대상 ID"),
                                fieldWithPath("data.reason").type(JsonFieldType.STRING).description("사유"),
                                fieldWithPath("data.detail").type(JsonFieldType.STRING).optional().description("자세한 내용"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태 (PENDING)"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("접수 시각")))));
    }

    @Test
    @DisplayName("신고하기: 같은 대상을 다시 신고하면 409 DUPLICATE_REPORT")
    void duplicate() throws Exception {
        given(reportService.report(eq(USER_ID), any(ReportRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_REPORT));

        mockMvc.perform(post("/api/v1/reports").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReportRequest(ReportTargetType.POST, 5L, ReportReason.SPAM, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REPORT"))
                .andDo(document("report-create-duplicate", resource("Report", "신고하기", "중복 신고 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("관리자: 신고 목록 · 상세 · 처리")
    void adminReports() throws Exception {
        given(adminReportService.getReports(eq(ReportStatus.PENDING), isNull(), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(adminReport(ReportStatus.PENDING, null)), 0, 20, 1, 1, false));
        given(adminReportService.getReport(1L)).willReturn(adminReport(ReportStatus.PENDING, null));
        given(adminReportService.resolve(eq(ADMIN_ID), eq(1L), any(ResolveReportRequest.class)))
                .willReturn(adminReport(ReportStatus.RESOLVED, ReportAction.HIDE_AND_SUSPEND));

        mockMvc.perform(get("/api/v1/admin/reports").param("status", "PENDING").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-report-list",
                        resource("Admin Report", "신고 목록", "status=PENDING 이면 오래된 순, 그 밖은 최근 순."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("status").optional().description("처리 상태 (선택지 reportStatuses)"),
                                parameterWithName("targetType").optional().description("대상 종류 (선택지 reportTargetTypes)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(adminFields("data.content[].")))));

        mockMvc.perform(get("/api/v1/admin/reports/{reportId}", 1L).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-report-detail",
                        resource("Admin Report", "신고 상세", "대상이 지워졌으면 대상 정보는 null 입니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("reportId").description("신고 ID")),
                        responseFields(success(adminFields("data.")))));

        mockMvc.perform(post("/api/v1/admin/reports/{reportId}/resolve", 1L).header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ResolveReportRequest(ReportAction.HIDE_AND_SUSPEND, 7, "허위 사진 확인, 1주 정지"))))
                .andExpect(status().isOk())
                .andDo(document("admin-report-resolve",
                        resource("Admin Report", "신고 처리", "같은 대상의 처리 대기 신고도 함께 닫힙니다. 숨긴 공간은 이용자에게 404, "
                                + "제공자는 수정할 수 없습니다. 이미 처리된 신고 409 REPORT_ALREADY_RESOLVED, 회원 신고 숨김 400 INVALID_REPORT_ACTION."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("reportId").description("신고 ID")),
                        requestFields(
                                fieldWithPath("action").type(JsonFieldType.STRING).description("DISMISS | HIDE | SUSPEND | HIDE_AND_SUSPEND"),
                                fieldWithPath("suspendDays").type(JsonFieldType.NUMBER).optional().description("이용 정지 일수 (정지가 들어간 처리만, 1~365)"),
                                fieldWithPath("note").type(JsonFieldType.STRING).optional().description("관리자 메모 (숨긴 공간의 사유로도 쓰임)")),
                        responseFields(success(adminFields("data.")))));
    }

    @Test
    @DisplayName("관리자: 회원 이용 정지 · 해제")
    void suspension() throws Exception {
        given(adminReportService.suspend(2L, 7)).willReturn(new MemberSuspensionResponse(2L, "김제공",
                MemberStatus.SUSPENDED, FixedClockConfig.FIXED_LOCAL_NOW.plusDays(7)));
        given(adminReportService.unsuspend(2L)).willReturn(new MemberSuspensionResponse(2L, "김제공",
                MemberStatus.ACTIVE, null));
        FieldDescriptor[] fields = {
                fieldWithPath("data.memberId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태 (ACTIVE | SUSPENDED | WITHDRAWN)"),
                fieldWithPath("data.suspendedUntil").type(JsonFieldType.STRING).optional().description("정지 해제 예정 시각")};

        mockMvc.perform(post("/api/v1/admin/members/{memberId}/suspend", 2L).header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content(json(new SuspendRequest(7))))
                .andExpect(status().isOk())
                .andDo(document("admin-member-suspend",
                        resource("Admin Report", "회원 이용 정지", "로그인 · 토큰 재발급이 막힙니다 (403 ACCOUNT_SUSPENDED). "
                                + "관리자 · 탈퇴 회원은 409 CANNOT_SUSPEND_MEMBER."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        requestFields(fieldWithPath("days").type(JsonFieldType.NUMBER).description("정지 일수 (1~365)")),
                        responseFields(success(fields))));

        mockMvc.perform(post("/api/v1/admin/members/{memberId}/unsuspend", 2L).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-member-unsuspend", resource("Admin Report", "이용 정지 해제", "바로 정상 상태가 됩니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        responseFields(success(fields))));
    }

    @Test
    @DisplayName("관리자가 아니면 신고 처리 API 403")
    void adminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isForbidden());
    }

    private static AdminReportResponse adminReport(ReportStatus status, ReportAction action) {
        return new AdminReportResponse(1L, ReportTargetType.SPACE, 10L, "정왕역 앞 1층 상가", action == null, PROVIDER_ID,
                "김제공", action == null ? MemberStatus.ACTIVE : MemberStatus.SUSPENDED, "이이용",
                ReportReason.FALSE_INFORMATION, "사진과 실제 공간이 전혀 다릅니다.", status, action,
                action == null ? null : "허위 사진 확인, 1주 정지", FixedClockConfig.FIXED_LOCAL_NOW,
                action == null ? null : FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static FieldDescriptor[] adminFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("신고 ID"),
                fieldWithPath(prefix + "targetType").type(JsonFieldType.STRING).description("대상 종류"),
                fieldWithPath(prefix + "targetId").type(JsonFieldType.NUMBER).description("대상 ID"),
                fieldWithPath(prefix + "targetSummary").type(JsonFieldType.STRING).optional().description("대상 요약 (지워졌으면 null)"),
                fieldWithPath(prefix + "targetVisible").type(JsonFieldType.BOOLEAN).description("대상이 아직 보이는지"),
                fieldWithPath(prefix + "ownerId").type(JsonFieldType.NUMBER).optional().description("대상 작성자 · 제공자 ID"),
                fieldWithPath(prefix + "ownerName").type(JsonFieldType.STRING).optional().description("대상 작성자 · 제공자 이름"),
                fieldWithPath(prefix + "ownerStatus").type(JsonFieldType.STRING).optional().description("대상 작성자 · 제공자 상태"),
                fieldWithPath(prefix + "reporterName").type(JsonFieldType.STRING).description("신고한 회원 이름"),
                fieldWithPath(prefix + "reason").type(JsonFieldType.STRING).description("사유"),
                fieldWithPath(prefix + "detail").type(JsonFieldType.STRING).optional().description("자세한 내용"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING).description("처리 상태"),
                fieldWithPath(prefix + "action").type(JsonFieldType.STRING).optional().description("처리 내용"),
                fieldWithPath(prefix + "adminNote").type(JsonFieldType.STRING).optional().description("관리자 메모"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("접수 시각"),
                fieldWithPath(prefix + "resolvedAt").type(JsonFieldType.STRING).optional().description("처리 시각")};
    }
}
