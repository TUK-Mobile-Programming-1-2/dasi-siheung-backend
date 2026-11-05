package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.report.AdminReportResponse;
import com.dasisiheung.dto.report.MemberSuspensionResponse;
import com.dasisiheung.dto.report.ReportPhotoResponse;
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
import com.dasisiheung.support.TestFiles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest({ReportController.class, AdminReportController.class})
class ReportControllerTest extends ApiDocsTestSupport {

    private static final String SIGNED_URL =
            "https://abc.supabase.co/storage/v1/object/sign/private-files/reports/1/photos/a.jpg?token=x";

    /** 같은 API 의 오류 예시도 같은 설명을 써야 명세의 설명이 예시 문구로 덮이지 않는다. */
    private static final String CREATE_DESCRIPTION = "로그인 회원 누구나. multipart/form-data 로 보냅니다. "
            + "파트: targetType · targetId · reason · detail(선택) 텍스트, photos(증거 사진, 선택, 최대 5장, JPG·PNG·WEBP 10MB). "
            + "대상이 없으면 404 REPORT_TARGET_NOT_FOUND, 자기 자신 · 자기 글은 400 CANNOT_REPORT_SELF, "
            + "처리 대기 중 같은 신고는 409 DUPLICATE_REPORT, 사진 5장 초과 400 REPORT_PHOTO_LIMIT_EXCEEDED, "
            + "사진 형식 400 UNSUPPORTED_FILE_TYPE · 크기 413 FILE_TOO_LARGE. 사진 하나라도 실패하면 신고도 접수되지 않습니다.";

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private AdminReportService adminReportService;

    @Test
    @DisplayName("신고하기 (증거 사진 첨부)")
    void report() throws Exception {
        given(reportService.report(eq(USER_ID), any(ReportRequest.class))).willReturn(new ReportResponse(1L,
                ReportTargetType.SPACE, 10L, ReportReason.FALSE_INFORMATION, "사진과 실제 공간이 전혀 다릅니다.",
                ReportStatus.PENDING, photos(), FixedClockConfig.FIXED_LOCAL_NOW));

        mockMvc.perform(multipart("/api/v1/reports")
                        .file(TestFiles.jpeg("photos"))
                        .file(TestFiles.jpeg("photos"))
                        .part(text("targetType", "SPACE"), text("targetId", "10"), text("reason", "FALSE_INFORMATION"),
                                text("detail", "사진과 실제 공간이 전혀 다릅니다."))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photos.length()").value(2))
                .andDo(document("report-create",
                        resource("Report", "신고하기", CREATE_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestParts(
                                partWithName("targetType").description("SPACE | POST | COMMENT | MEMBER"),
                                partWithName("targetId").description("대상 ID"),
                                partWithName("reason").description("사유 (선택지 reportReasons)"),
                                partWithName("detail").optional().description("자세한 내용 (선택, 최대 1000자)"),
                                partWithName("photos").optional()
                                        .description("증거 사진 (선택, 같은 이름으로 여러 개, 최대 5장, JPG·PNG·WEBP 10MB)")),
                        responseFields(success(
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("신고 ID"),
                                fieldWithPath("data.targetType").type(JsonFieldType.STRING).description("대상 종류"),
                                fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("대상 ID"),
                                fieldWithPath("data.reason").type(JsonFieldType.STRING).description("사유"),
                                fieldWithPath("data.detail").type(JsonFieldType.STRING).optional().description("자세한 내용"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("처리 상태 (PENDING)"),
                                fieldWithPath("data.photos[]").type(JsonFieldType.ARRAY).description("첨부한 증거 사진 (올린 순, 없으면 빈 배열)"),
                                fieldWithPath("data.photos[].id").type(JsonFieldType.NUMBER).description("사진 ID"),
                                fieldWithPath("data.photos[].url").type(JsonFieldType.STRING).description("열람용 임시 주소 (10분 뒤 만료)"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("접수 시각")))));

        ArgumentCaptor<ReportRequest> captor = ArgumentCaptor.forClass(ReportRequest.class);
        verify(reportService).report(eq(USER_ID), captor.capture());
        ReportRequest request = captor.getValue();
        assertThat(request.targetType()).isEqualTo(ReportTargetType.SPACE);
        assertThat(request.targetId()).isEqualTo(10L);
        assertThat(request.reason()).isEqualTo(ReportReason.FALSE_INFORMATION);
        assertThat(request.detail()).isEqualTo("사진과 실제 공간이 전혀 다릅니다.");
        assertThat(request.attachedPhotos()).hasSize(2);
    }

    @Test
    @DisplayName("신고하기: 사진 없이도 접수된다")
    void reportWithoutPhotos() throws Exception {
        given(reportService.report(eq(USER_ID), any(ReportRequest.class))).willReturn(new ReportResponse(2L,
                ReportTargetType.COMMENT, 5L, ReportReason.SPAM, null, ReportStatus.PENDING, List.of(),
                FixedClockConfig.FIXED_LOCAL_NOW));

        mockMvc.perform(multipart("/api/v1/reports")
                        .part(text("targetType", "COMMENT"), text("targetId", "5"), text("reason", "SPAM"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photos").isEmpty());

        ArgumentCaptor<ReportRequest> captor = ArgumentCaptor.forClass(ReportRequest.class);
        verify(reportService).report(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().attachedPhotos()).isEmpty();
    }

    @Test
    @DisplayName("신고하기: 같은 대상을 다시 신고하면 409 DUPLICATE_REPORT")
    void duplicate() throws Exception {
        given(reportService.report(eq(USER_ID), any(ReportRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_REPORT));

        mockMvc.perform(multipart("/api/v1/reports")
                        .part(text("targetType", "POST"), text("targetId", "5"), text("reason", "SPAM"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REPORT"))
                .andDo(document("report-create-duplicate", resource("Report", "신고하기", CREATE_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("신고하기: 사진이 5장을 넘으면 400 REPORT_PHOTO_LIMIT_EXCEEDED")
    void tooManyPhotos() throws Exception {
        given(reportService.report(eq(USER_ID), any(ReportRequest.class)))
                .willThrow(new BusinessException(ErrorCode.REPORT_PHOTO_LIMIT_EXCEEDED));

        mockMvc.perform(multipart("/api/v1/reports")
                        .file(TestFiles.jpeg("photos"))
                        .part(text("targetType", "SPACE"), text("targetId", "10"), text("reason", "FRAUD"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REPORT_PHOTO_LIMIT_EXCEEDED"))
                .andDo(document("report-create-photo-limit", resource("Report", "신고하기", CREATE_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("신고하기: 필수 값이 없으면 400 INVALID_INPUT + 필드 오류")
    void missingRequired() throws Exception {
        mockMvc.perform(multipart("/api/v1/reports")
                        .part(text("targetId", "10"), text("reason", "SPAM"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("targetType"));
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("신고하기: 형식이 틀리면 400 INVALID_INPUT, 사유에 내부 클래스 이름을 드러내지 않는다")
    void typeMismatch() throws Exception {
        mockMvc.perform(multipart("/api/v1/reports")
                        .part(text("targetType", "SPACE"), text("targetId", "abc"), text("reason", "NOT_A_REASON"))
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.error.fieldErrors[*].reason").value(everyItem(is("값의 형식이 올바르지 않습니다."))));
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("신고하기: 예전 JSON 요청은 415 UNSUPPORTED_MEDIA_TYPE")
    void jsonNoLongerSupported() throws Exception {
        mockMvc.perform(post("/api/v1/reports").header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\": \"SPACE\", \"targetId\": 10, \"reason\": \"SPAM\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("관리자: 신고 목록 · 상세 · 처리")
    void adminReports() throws Exception {
        given(adminReportService.getReports(eq(ReportStatus.PENDING), isNull(), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(adminReport(ReportStatus.PENDING, null, null)), 0, 20, 1, 1, false));
        given(adminReportService.getReport(1L)).willReturn(adminReport(ReportStatus.PENDING, null, photos()));
        given(adminReportService.resolve(eq(ADMIN_ID), eq(1L), any(ResolveReportRequest.class)))
                .willReturn(adminReport(ReportStatus.RESOLVED, ReportAction.HIDE_AND_SUSPEND, photos()));

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
                        responseFields(page(adminFields("data.content[].", false)))));

        mockMvc.perform(get("/api/v1/admin/reports/{reportId}", 1L).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-report-detail",
                        resource("Admin Report", "신고 상세", "대상이 지워졌으면 대상 정보는 null 입니다. "
                                + "증거 사진 임시 주소는 10분 뒤 만료되므로 화면을 열 때마다 조회하세요."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("reportId").description("신고 ID")),
                        responseFields(success(adminFields("data.", true)))));

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
                        responseFields(success(adminFields("data.", true)))));
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

    /** @param photos 상세 · 처리 응답이면 사진 목록, 목록 응답이면 null */
    private static AdminReportResponse adminReport(ReportStatus status, ReportAction action,
                                                   List<ReportPhotoResponse> photos) {
        return new AdminReportResponse(1L, ReportTargetType.SPACE, 10L, "정왕역 앞 1층 상가", action == null, PROVIDER_ID,
                "김제공", action == null ? MemberStatus.ACTIVE : MemberStatus.SUSPENDED, "이이용",
                ReportReason.FALSE_INFORMATION, "사진과 실제 공간이 전혀 다릅니다.", 2, photos, status, action,
                action == null ? null : "허위 사진 확인, 1주 정지", FixedClockConfig.FIXED_LOCAL_NOW,
                action == null ? null : FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static List<ReportPhotoResponse> photos() {
        return List.of(new ReportPhotoResponse(1L, SIGNED_URL), new ReportPhotoResponse(2L, SIGNED_URL));
    }

    private static MockPart text(String name, String value) {
        return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
    }

    /** @param withPhotos 상세 · 처리 응답이면 true (사진 주소 포함), 목록이면 false (photos 는 null) */
    private static FieldDescriptor[] adminFields(String prefix, boolean withPhotos) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
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
                fieldWithPath(prefix + "photoCount").type(JsonFieldType.NUMBER).description("증거 사진 장수"),
                fieldWithPath(prefix + "status").type(JsonFieldType.STRING).description("처리 상태"),
                fieldWithPath(prefix + "action").type(JsonFieldType.STRING).optional().description("처리 내용"),
                fieldWithPath(prefix + "adminNote").type(JsonFieldType.STRING).optional().description("관리자 메모"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("접수 시각"),
                fieldWithPath(prefix + "resolvedAt").type(JsonFieldType.STRING).optional().description("처리 시각")));
        if (withPhotos) {
            fields.add(fieldWithPath(prefix + "photos[]").type(JsonFieldType.ARRAY).description("증거 사진 (올린 순)"));
            fields.add(fieldWithPath(prefix + "photos[].id").type(JsonFieldType.NUMBER).description("사진 ID"));
            fields.add(fieldWithPath(prefix + "photos[].url").type(JsonFieldType.STRING).description("열람용 임시 주소 (10분 뒤 만료)"));
        } else {
            fields.add(fieldWithPath(prefix + "photos").type(JsonFieldType.NULL).optional()
                    .description("목록에서는 null (사진은 상세에서 확인)"));
        }
        return fields.toArray(FieldDescriptor[]::new);
    }
}
