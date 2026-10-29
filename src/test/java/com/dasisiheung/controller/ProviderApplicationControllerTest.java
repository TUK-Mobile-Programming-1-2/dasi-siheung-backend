package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.ApiDocumentation.validationError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.dasisiheung.dto.application.RejectApplicationRequest;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.ProviderApplicationService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.ApplicationDocumentation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(ProviderApplicationController.class)
class ProviderApplicationControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Provider Application";

    @MockitoBean
    private ProviderApplicationService providerApplicationService;

    @Test
    @DisplayName("받은 신청 목록")
    void getReceivedApplications() throws Exception {
        given(providerApplicationService.getReceivedApplications(eq(PROVIDER_ID), eq(ApplicationStatus.PENDING),
                eq(10L), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(ApplicationControllerTest.summary()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/provider/applications").param("status", "PENDING").param("spaceId", "10")
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].applicantName").value("이이용"))
                .andDo(document("provider-application-list",
                        resource(TAG, "받은 신청 목록", "최근 순입니다. 상세 · 가격 협의는 /api/v1/applications/{id} 를 씁니다."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("status").optional().description("신청 상태 필터 (선택지 applicationStatuses)"),
                                parameterWithName("spaceId").optional().description("공간 필터 (생략하면 내 모든 공간)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(ApplicationDocumentation.summaryFields("data.content[].")))));
    }

    @Test
    @DisplayName("신청 승인 (예약 확정)")
    void approve() throws Exception {
        given(providerApplicationService.approve(PROVIDER_ID, 7L)).willReturn(ApplicationControllerTest.detail());

        mockMvc.perform(post("/api/v1/provider/applications/{applicationId}/approve", 7L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("provider-application-approve",
                        resource(TAG, "신청 승인 (예약 확정)", "검토 중 · 가격 협의가 끝난 신청만 승인할 수 있습니다. "
                                + "겹치는 기간의 다른 검토 중 신청은 자동으로 마감(CLOSED)되고 사유가 남습니다. "
                                + "오류: 협의 대기 409 PRICE_NEGOTIATION_IN_PROGRESS, 이미 확정된 기간 409 PERIOD_ALREADY_BOOKED, "
                                + "캘린더에서 빠진 날짜 409 PERIOD_NOT_AVAILABLE, 시작일 지남 400 INVALID_APPLICATION_PERIOD, "
                                + "끝난 신청 409 INVALID_APPLICATION_STATUS."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청 ID")),
                        responseFields(success(ApplicationControllerTest.fields(
                                ApplicationDocumentation.detailFields("data."))))));
    }

    @Test
    @DisplayName("신청 승인: 이미 확정된 기간이면 409 PERIOD_ALREADY_BOOKED")
    void approveAlreadyBooked() throws Exception {
        given(providerApplicationService.approve(PROVIDER_ID, 8L))
                .willThrow(new BusinessException(ErrorCode.PERIOD_ALREADY_BOOKED));

        mockMvc.perform(post("/api/v1/provider/applications/{applicationId}/approve", 8L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_ALREADY_BOOKED"))
                .andDo(document("provider-application-approve-already-booked",
                        resource(TAG, "신청 승인 (예약 확정)", "같은 기간에 이미 확정된 예약이 있는 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("신청 거절")
    void reject() throws Exception {
        given(providerApplicationService.reject(eq(PROVIDER_ID), eq(7L), any(RejectApplicationRequest.class)))
                .willReturn(ApplicationControllerTest.detail());

        mockMvc.perform(post("/api/v1/provider/applications/{applicationId}/reject", 7L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RejectApplicationRequest("해당 기간에 내부 공사가 잡혀 있습니다."))))
                .andExpect(status().isOk())
                .andDo(document("provider-application-reject",
                        resource(TAG, "신청 거절", "검토 중인 신청만 (아니면 409 INVALID_APPLICATION_STATUS). "
                                + "내 공간에 온 신청이 아니면 403 NOT_APPLICATION_PARTICIPANT. 사유는 신청자에게 보입니다."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청 ID")),
                        requestFields(fieldWithPath("reason").type(JsonFieldType.STRING).description("거절 사유 (필수, 최대 500자)")),
                        responseFields(success(ApplicationControllerTest.fields(
                                ApplicationDocumentation.detailFields("data."))))));
    }

    @Test
    @DisplayName("신청 거절: 사유가 비면 400 INVALID_INPUT")
    void rejectWithoutReason() throws Exception {
        mockMvc.perform(post("/api/v1/provider/applications/{applicationId}/reject", 7L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andDo(document("provider-application-reject-without-reason",
                        resource(TAG, "신청 거절", "사유 없이 거절한 예시"),
                        responseFields(validationError())));
    }

    @Test
    @DisplayName("이용자는 받은 신청 API 를 쓸 수 없다 (403)")
    void userForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/provider/applications").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isForbidden());
    }
}
