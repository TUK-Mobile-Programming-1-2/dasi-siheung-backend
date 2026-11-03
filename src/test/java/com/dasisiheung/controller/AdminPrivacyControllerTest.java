package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.privacy.MonthlyStatisticsResponse;
import com.dasisiheung.dto.privacy.PurgePreviewResponse;
import com.dasisiheung.dto.privacy.PurgeResultResponse;
import com.dasisiheung.entity.space.ActivityType;
import com.dasisiheung.entity.space.Region;
import com.dasisiheung.service.PrivacyService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(AdminPrivacyController.class)
class AdminPrivacyControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Admin Privacy";
    private static final LocalDate TODAY = FixedClockConfig.FIXED_TODAY;

    @MockitoBean
    private PrivacyService privacyService;

    @Test
    @DisplayName("파기 미리보기 · 실행 · 기록")
    void purge() throws Exception {
        given(privacyService.preview()).willReturn(new PurgePreviewResponse(TODAY.minusDays(365), 12,
                TODAY.minusDays(365).atStartOfDay(), 3, TODAY.minusDays(90), 20));
        PurgeResultResponse result = new PurgeResultResponse(1L, ADMIN_ID, TODAY.minusDays(365), 5, 12, 3, 20,
                FixedClockConfig.FIXED_LOCAL_NOW);
        given(privacyService.purge(ADMIN_ID)).willReturn(result);
        given(privacyService.getLogs(any(PageQuery.class))).willReturn(new PageResponse<>(List.of(result), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/admin/privacy/purge-preview").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-privacy-preview",
                        resource(TAG, "파기 미리보기", "보관 기간: 신청서 글 이용 종료 후 365일, 1:1 대화 마지막 메시지 후 365일, "
                                + "활동 사진 이용 종료 후 90일 (app.privacy.* 로 조정)."),
                        requestHeaders(authorization()),
                        responseFields(success(
                                fieldWithPath("data.applicationCutoff").type(JsonFieldType.STRING).description("이 날짜 전에 이용이 끝난 신청서를 비식별화"),
                                fieldWithPath("data.applicationsToAnonymize").type(JsonFieldType.NUMBER).description("비식별화할 신청 수"),
                                fieldWithPath("data.conversationCutoff").type(JsonFieldType.STRING).description("이 시각 전부터 대화가 없던 1:1 대화 삭제"),
                                fieldWithPath("data.conversationsToDelete").type(JsonFieldType.NUMBER).description("삭제할 대화 수"),
                                fieldWithPath("data.activityPhotoCutoff").type(JsonFieldType.STRING).description("이 날짜 전에 이용이 끝난 예약의 활동 사진 삭제"),
                                fieldWithPath("data.activityPhotosToDelete").type(JsonFieldType.NUMBER).description("삭제할 활동 사진 수")))));

        mockMvc.perform(post("/api/v1/admin/privacy/purge").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.anonymizedApplications").value(12))
                .andDo(document("admin-privacy-purge",
                        resource(TAG, "파기 실행", "통계 적립 → 신청서 글 비식별화 → 대화 · 활동 사진 삭제. 매일 04:30 자동 실행되며 여러 번 실행해도 결과가 같습니다."),
                        requestHeaders(authorization()),
                        responseFields(success(resultFields("data.")))));

        mockMvc.perform(get("/api/v1/admin/privacy/purge-logs").header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-privacy-logs", resource(TAG, "파기 실행 기록", "최근 순. executedBy 가 null 이면 자동 실행."),
                        requestHeaders(authorization()),
                        responseFields(page(resultFields("data.content[].")))));
    }

    @Test
    @DisplayName("월별 이용 통계")
    void monthlyStatistics() throws Exception {
        given(privacyService.getMonthlyStatistics(YearMonth.of(2026, 1), YearMonth.of(2026, 12)))
                .willReturn(new MonthlyStatisticsResponse(YearMonth.of(2026, 1), YearMonth.of(2026, 12), 4, 9, 430_000,
                        List.of(new MonthlyStatisticsResponse.Row(YearMonth.of(2026, 10), Region.JEONGWANG,
                                ActivityType.POPUP_STORE, 4, 9, 430_000))));

        mockMvc.perform(get("/api/v1/admin/statistics/monthly").param("from", "2026-01").param("to", "2026-12")
                        .header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].month").value("2026-10"))
                .andDo(document("admin-statistics-monthly",
                        resource(TAG, "월별 이용 통계", "어제까지 이용이 끝난 확정 예약 기준, 개인 식별 정보 없이 수치만. 최대 36개월."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("from").description("시작 월 yyyy-MM"),
                                parameterWithName("to").description("끝 월 yyyy-MM")),
                        responseFields(success(
                                fieldWithPath("data.from").type(JsonFieldType.STRING).description("시작 월"),
                                fieldWithPath("data.to").type(JsonFieldType.STRING).description("끝 월"),
                                fieldWithPath("data.totalBookings").type(JsonFieldType.NUMBER).description("확정 예약 수 합계"),
                                fieldWithPath("data.totalBookedDays").type(JsonFieldType.NUMBER).description("이용 일수 합계"),
                                fieldWithPath("data.totalAmount").type(JsonFieldType.NUMBER).description("합의 금액 합계(원)"),
                                fieldWithPath("data.rows[].month").type(JsonFieldType.STRING).description("월 yyyy-MM"),
                                fieldWithPath("data.rows[].region").type(JsonFieldType.STRING).description("지역"),
                                fieldWithPath("data.rows[].activityType").type(JsonFieldType.STRING).description("활동"),
                                fieldWithPath("data.rows[].bookingCount").type(JsonFieldType.NUMBER).description("확정 예약 수"),
                                fieldWithPath("data.rows[].bookedDays").type(JsonFieldType.NUMBER).description("이용 일수"),
                                fieldWithPath("data.rows[].totalAmount").type(JsonFieldType.NUMBER).description("합의 금액 합계(원)")))));
    }

    private static FieldDescriptor[] resultFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("기록 ID"),
                fieldWithPath(prefix + "executedBy").type(JsonFieldType.NUMBER).optional().description("실행한 관리자 ID (자동 실행이면 null)"),
                fieldWithPath(prefix + "applicationCutoff").type(JsonFieldType.STRING).description("신청서 비식별화 기준일"),
                fieldWithPath(prefix + "countedBookings").type(JsonFieldType.NUMBER).description("통계에 더한 확정 예약 수"),
                fieldWithPath(prefix + "anonymizedApplications").type(JsonFieldType.NUMBER).description("비식별화한 신청 수"),
                fieldWithPath(prefix + "deletedConversations").type(JsonFieldType.NUMBER).description("삭제한 대화 수"),
                fieldWithPath(prefix + "deletedActivityPhotos").type(JsonFieldType.NUMBER).description("삭제한 활동 사진 수"),
                fieldWithPath(prefix + "executedAt").type(JsonFieldType.STRING).description("실행 시각")};
    }
}
