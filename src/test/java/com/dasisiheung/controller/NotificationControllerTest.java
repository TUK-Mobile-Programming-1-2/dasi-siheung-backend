package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.page;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.notification.NotificationCountResponse;
import com.dasisiheung.dto.notification.NotificationResponse;
import com.dasisiheung.entity.notification.NotificationType;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.NotificationService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(NotificationController.class)
class NotificationControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Notification";
    private static final LocalDateTime NOW = FixedClockConfig.FIXED_LOCAL_NOW;
    private static final String COUNTER_OFFER_BODY = "제공자님이 '정왕역 앞 1층 상가' 10/23~10/24 이용 금액으로 95,000원을 제안했어요.";

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("내 알림 목록 (안 읽은 알림만)")
    void getMyNotifications() throws Exception {
        given(notificationService.getMyNotifications(eq(USER_ID), eq(true), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(unread()), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", "true")
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("PRICE_OFFER_RECEIVED"))
                .andExpect(jsonPath("$.data.content[0].applicationId").value(31))
                .andExpect(jsonPath("$.data.content[0].read").value(false))
                .andDo(document("notification-list",
                        resource(TAG, "내 알림 목록", "로그인한 회원의 알림함 (최근 순). 가격 제안 · 수락 · 거절이 남으며, "
                                + "푸시를 받지 못했어도(알림 끔 · 방해금지 · 기기 없음) 여기에는 항상 있습니다. "
                                + "항목을 누르면 applicationId 의 신청 상세(GET /api/v1/applications/{id})를 열고 읽음 처리하세요."),
                        requestHeaders(authorization()),
                        queryParameters(
                                parameterWithName("unreadOnly").optional().description("true 면 안 읽은 알림만 (기본 false)"),
                                parameterWithName("page").optional().description("페이지 (0부터)"),
                                parameterWithName("size").optional().description("페이지 크기 (최대 100)")),
                        responseFields(page(fields("data.content[].")))));
    }

    @Test
    @DisplayName("내 알림 목록: unreadOnly 를 생략하면 전체")
    void getAllNotifications() throws Exception {
        given(notificationService.getMyNotifications(eq(PROVIDER_ID), eq(false), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, false));

        mockMvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        then(notificationService).should().getMyNotifications(eq(PROVIDER_ID), eq(false), any(PageQuery.class));
    }

    @Test
    @DisplayName("내 알림 목록: size 가 100 을 넘으면 400 INVALID_INPUT")
    void tooLargePage() throws Exception {
        mockMvc.perform(get("/api/v1/notifications").param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("안 읽은 알림 수")
    void getUnreadCount() throws Exception {
        given(notificationService.countUnread(USER_ID)).willReturn(new NotificationCountResponse(2));

        mockMvc.perform(get("/api/v1/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andDo(document("notification-unread-count",
                        resource(TAG, "안 읽은 알림 수", "알림 아이콘 배지용입니다."),
                        requestHeaders(authorization()),
                        responseFields(success(fieldWithPath("data.count").type(JsonFieldType.NUMBER)
                                .description("안 읽은 알림 수")))));
    }

    @Test
    @DisplayName("알림 읽음 처리")
    void markRead() throws Exception {
        given(notificationService.markRead(USER_ID, 12L)).willReturn(read());

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", 12L)
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andDo(document("notification-read",
                        resource(TAG, "알림 읽음 처리", "푸시 data 의 notificationId 또는 목록의 id 를 씁니다. "
                                + "이미 읽은 알림이면 처음 읽은 시각을 그대로 돌려줍니다. "
                                + "없거나 남의 알림이면 404 NOTIFICATION_NOT_FOUND."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("notificationId").description("알림 ID")),
                        responseFields(success(fields("data.")))));
    }

    @Test
    @DisplayName("알림 읽음 처리: 남의 알림이면 404 NOTIFICATION_NOT_FOUND")
    void markOthersRead() throws Exception {
        given(notificationService.markRead(PROVIDER_ID, 12L))
                .willThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", 12L)
                        .header(HttpHeaders.AUTHORIZATION, providerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"))
                .andDo(document("notification-read-not-found",
                        resource(TAG, "알림 읽음 처리", "남의 알림을 읽음 처리하려 한 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("모두 읽음 처리")
    void markAllRead() throws Exception {
        given(notificationService.markAllRead(USER_ID)).willReturn(new NotificationCountResponse(2));

        mockMvc.perform(post("/api/v1/notifications/read-all").header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andDo(document("notification-read-all",
                        resource(TAG, "모두 읽음 처리", "안 읽은 내 알림을 모두 읽음으로 바꿉니다."),
                        requestHeaders(authorization()),
                        responseFields(success(fieldWithPath("data.count").type(JsonFieldType.NUMBER)
                                .description("이번에 읽음으로 바뀐 알림 수")))));
    }

    @Test
    @DisplayName("로그인 없이 호출하면 401 UNAUTHORIZED")
    void withoutLogin() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("page · size 를 그대로 넘긴다")
    void passesPageQuery() throws Exception {
        given(notificationService.getMyNotifications(eq(USER_ID), eq(false), any(PageQuery.class)))
                .willReturn(new PageResponse<>(List.of(), 2, 5, 0, 0, false));

        mockMvc.perform(get("/api/v1/notifications").param("page", "2").param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> pageQuery = ArgumentCaptor.forClass(PageQuery.class);
        then(notificationService).should().getMyNotifications(eq(USER_ID), eq(false), pageQuery.capture());
        assertThat(pageQuery.getValue()).isEqualTo(new PageQuery(2, 5));
    }

    private static NotificationResponse unread() {
        return new NotificationResponse(12L, NotificationType.PRICE_OFFER_RECEIVED, "공간 제공자의 역제안",
                COUNTER_OFFER_BODY, 31L, false, null, NOW);
    }

    private static NotificationResponse read() {
        return new NotificationResponse(12L, NotificationType.PRICE_OFFER_RECEIVED, "공간 제공자의 역제안",
                COUNTER_OFFER_BODY, 31L, true, NOW.plusMinutes(3), NOW);
    }

    private static FieldDescriptor[] fields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("알림 ID (읽음 처리에 사용)"),
                fieldWithPath(prefix + "type").type(JsonFieldType.STRING).description(
                        "PRICE_OFFER_RECEIVED 상대의 가격 제안 · 역제안 도착 (수락 · 거절 · 역제안할 차례) | "
                                + "PRICE_OFFER_ACCEPTED 내 제안이 수락됨 | PRICE_OFFER_REJECTED 내 제안이 거절됨 (다시 제안 가능). "
                                + "푸시 data 의 type 과 같습니다"),
                fieldWithPath(prefix + "title").type(JsonFieldType.STRING).description("제목"),
                fieldWithPath(prefix + "body").type(JsonFieldType.STRING).description("본문"),
                fieldWithPath(prefix + "applicationId").type(JsonFieldType.NUMBER).description("연결된 신청 ID — 누르면 신청 상세로 이동"),
                fieldWithPath(prefix + "read").type(JsonFieldType.BOOLEAN).description("읽었는지"),
                fieldWithPath(prefix + "readAt").type(JsonFieldType.STRING).optional().description("처음 읽은 시각 (안 읽었으면 null)"),
                fieldWithPath(prefix + "createdAt").type(JsonFieldType.STRING).description("알림 시각")};
    }
}
