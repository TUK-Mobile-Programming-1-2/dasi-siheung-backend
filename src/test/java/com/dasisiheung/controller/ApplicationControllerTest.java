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
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.application.ApplicationRequest;
import com.dasisiheung.dto.application.ApplicationResponse;
import com.dasisiheung.dto.application.ApplicationSummaryResponse;
import com.dasisiheung.dto.application.CancelApplicationRequest;
import com.dasisiheung.dto.application.PriceOfferRequest;
import com.dasisiheung.dto.application.PriceOfferResponse;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.ApplicationStatus;
import com.dasisiheung.entity.application.OfferSide;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.ApplicationService;
import com.dasisiheung.service.PriceNegotiationService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.ApplicationDocumentation;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(ApplicationController.class)
class ApplicationControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Application";
    static final LocalDate START = FixedClockConfig.FIXED_TODAY.plusDays(3);
    static final LocalDate END = START.plusDays(1);

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private PriceNegotiationService priceNegotiationService;

    @Test
    @DisplayName("신청서 제출")
    void submit() throws Exception {
        given(applicationService.submit(eq(USER_ID), any(ApplicationRequest.class))).willReturn(detail());

        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ApplicationFixtures.request(10L, START, END))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.listTotalPrice").value(100_000))
                .andDo(document("application-submit",
                        resource(TAG, "신청서 제출", "이용자(USER)만 신청할 수 있습니다. 공개 공간이 아니면 404 SPACE_NOT_FOUND, "
                                + "허용하지 않는 활동 400 ACTIVITY_NOT_ALLOWED, 지난 날짜 · 30일 초과 400 INVALID_APPLICATION_PERIOD, "
                                + "사용 가능 날짜가 아닌 날 409 PERIOD_NOT_AVAILABLE, 같은 공간 · 기간의 검토 중 신청 409 DUPLICATE_APPLICATION."),
                        requestHeaders(authorization()),
                        requestFields(ApplicationDocumentation.requestFields()),
                        responseFields(success(fields(ApplicationDocumentation.detailFields("data."))))));
    }

    @Test
    @DisplayName("신청서 제출: 사용 가능 날짜가 아니면 409 PERIOD_NOT_AVAILABLE")
    void periodNotAvailable() throws Exception {
        given(applicationService.submit(eq(USER_ID), any(ApplicationRequest.class)))
                .willThrow(new BusinessException(ErrorCode.PERIOD_NOT_AVAILABLE));

        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ApplicationFixtures.request(10L, START, END))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_AVAILABLE"))
                .andDo(document("application-submit-period-not-available",
                        resource(TAG, "신청서 제출", "사용 가능 날짜가 아닌 날이 섞인 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("신청서 제출: 공간 제공자는 신청할 수 없다 (403)")
    void providerCannotSubmit() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ApplicationFixtures.request(10L, START, END))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("신청서 제출: 종료일이 시작일보다 빠르면 400 INVALID_INPUT")
    void reversedPeriod() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ApplicationFixtures.request(10L, END, START))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("내가 보낸 신청 목록")
    void getMyApplications() throws Exception {
        given(applicationService.getMyApplications(eq(USER_ID), eq(ApplicationStatus.PENDING), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(summary()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/applications").param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].spaceTitle").value("정왕역 앞 1층 상가"))
                .andDo(document("application-my-list",
                        resource(TAG, "내가 보낸 신청", "최근 순입니다."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("status").optional().description("신청 상태 필터 (선택지 applicationStatuses)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(ApplicationDocumentation.summaryFields("data.content[].")))));
    }

    @Test
    @DisplayName("신청 상세: 공간 제공자도 볼 수 있다")
    void getApplication() throws Exception {
        given(applicationService.getApplication(PROVIDER_ID, 7L)).willReturn(detail());

        mockMvc.perform(get("/api/v1/applications/{applicationId}", 7L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andDo(document("application-detail",
                        resource(TAG, "신청 상세", "신청자와 해당 공간 제공자만 볼 수 있습니다. (아니면 403 NOT_APPLICATION_PARTICIPANT)"),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청 ID")),
                        responseFields(success(fields(ApplicationDocumentation.detailFields("data."))))));
    }

    @Test
    @DisplayName("신청 취소")
    void cancel() throws Exception {
        given(applicationService.cancel(eq(USER_ID), eq(7L), any(CancelApplicationRequest.class)))
                .willReturn(detail());

        mockMvc.perform(post("/api/v1/applications/{applicationId}/cancel", 7L)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CancelApplicationRequest("일정이 바뀌었습니다."))))
                .andExpect(status().isOk())
                .andDo(document("application-cancel",
                        resource(TAG, "신청 취소", "검토 중이면 언제든, 확정된 예약은 이용 시작일 전까지 "
                                + "(아니면 409 APPLICATION_NOT_CANCELLABLE). 끝난 신청은 409 INVALID_APPLICATION_STATUS."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청 ID")),
                        requestFields(fieldWithPath("reason").type(JsonFieldType.STRING).optional()
                                .description("취소 사유 (선택, 최대 500자) — 공간 제공자에게 보입니다")),
                        responseFields(success(fields(ApplicationDocumentation.detailFields("data."))))));
    }

    @Test
    @DisplayName("가격 제안 · 역제안")
    void offer() throws Exception {
        given(priceNegotiationService.offer(eq(PROVIDER_ID), eq(7L), any(PriceOfferRequest.class))).willReturn(detail());

        mockMvc.perform(post("/api/v1/applications/{applicationId}/offers", 7L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PriceOfferRequest(95_000, "9만 5천원까지 가능합니다."))))
                .andExpect(status().isOk())
                .andDo(document("application-offer",
                        resource(TAG, "가격 제안 · 역제안", "신청자 · 공간 제공자 모두 보낼 수 있습니다. 검토 중인 신청만 "
                                + "(아니면 409 INVALID_APPLICATION_STATUS), 금액은 1원~정가 합계 (400 INVALID_OFFER_PRICE), "
                                + "내 제안에 상대가 답하기 전에는 다시 보낼 수 없습니다 (409 OFFER_NOT_ALLOWED)."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청 ID")),
                        requestFields(
                                fieldWithPath("amount").type(JsonFieldType.NUMBER).description("제안 금액(원, 기간 합계)"),
                                fieldWithPath("message").type(JsonFieldType.STRING).optional().description("한마디 (선택, 최대 300자)")),
                        responseFields(success(fields(ApplicationDocumentation.detailFields("data."))))));
    }

    @Test
    @DisplayName("가격 제안: 답을 기다리는 중에 다시 보내면 409 OFFER_NOT_ALLOWED")
    void offerNotAllowed() throws Exception {
        given(priceNegotiationService.offer(eq(USER_ID), eq(7L), any(PriceOfferRequest.class)))
                .willThrow(new BusinessException(ErrorCode.OFFER_NOT_ALLOWED));

        mockMvc.perform(post("/api/v1/applications/{applicationId}/offers", 7L)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new PriceOfferRequest(80_000, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OFFER_NOT_ALLOWED"))
                .andDo(document("application-offer-not-allowed",
                        resource(TAG, "가격 제안 · 역제안", "상대 답변 대기 중 다시 제안한 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("받은 가격 제안 수락")
    void acceptOffer() throws Exception {
        given(priceNegotiationService.accept(USER_ID, 7L)).willReturn(detail());

        mockMvc.perform(post("/api/v1/applications/{applicationId}/offers/accept", 7L)
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("application-offer-accept",
                        resource(TAG, "받은 가격 제안 수락", "상대가 보낸 최근 제안 금액이 합의 금액(totalPrice)이 됩니다. "
                                + "내가 답할 제안이 없으면 409 NO_PENDING_OFFER."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("applicationId").description("신청 ID")),
                        responseFields(success(fields(ApplicationDocumentation.detailFields("data."))))));
    }

    static Application application() {
        return ApplicationFixtures.application(7L,
                ApplicationFixtures.publishedSpace(SpaceFixtures.space(10L, SpaceFixtures.provider(PROVIDER_ID),
                        Set.of(START, END), FixedClockConfig.FIXED_TODAY)),
                ApplicationFixtures.applicant(USER_ID), START, END, FixedClockConfig.FIXED_TODAY);
    }

    static ApplicationResponse detail() {
        return ApplicationResponse.of(application(), ProviderSpaceControllerTest.photos().get(0).url(), List.of(
                new PriceOfferResponse(1L, OfferSide.APPLICANT, "이이용", 90_000, "조금만 조정 부탁드립니다.", null,
                        FixedClockConfig.FIXED_LOCAL_NOW)), 3L);
    }

    static ApplicationSummaryResponse summary() {
        return ApplicationSummaryResponse.of(application(), ProviderSpaceControllerTest.photos().get(0).url(), 3L);
    }

    static FieldDescriptor[] fields(List<FieldDescriptor> fields) {
        return fields.toArray(FieldDescriptor[]::new);
    }
}
