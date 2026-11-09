package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.ApiDocumentation.validationError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.member.NotificationSettingRequest;
import com.dasisiheung.dto.member.NotificationSettingResponse;
import com.dasisiheung.service.NotificationSettingService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import com.dasisiheung.support.FixedClockConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(NotificationSettingController.class)
class NotificationSettingControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "NotificationSetting";
    private static final String PATH = "/api/v1/members/me/notification-settings";
    private static final String GET_DESCRIPTION = "로그인한 회원의 알림 설정입니다. 저장한 적이 없으면 기본값"
            + "(push · chat · activity 켜짐, favoriteSpace · marketing 꺼짐, 방해금지 22시 → 8시)을 주고 updatedAt 은 null 입니다. "
            + "방해금지 · 분류별 수신 여부는 서버가 푸시를 보낼 때 지킵니다.";
    private static final String PUT_DESCRIPTION = "모든 항목을 보내야 합니다(전체 교체). 저장된 값을 돌려줍니다. "
            + "시각은 0~23 정수, 시작은 포함 · 끝은 포함하지 않으며 시작 > 끝이면 자정을 넘는 구간입니다(22 → 8 = 22:00~07:59). "
            + "방해금지를 켜면 시작과 끝이 달라야 합니다. 광고성 정보는 동의로 바뀔 때 서버가 marketingConsentedAt 을 남기고, "
            + "철회하면 null 이 됩니다. 항목이 빠지거나 범위를 벗어나면 400 INVALID_INPUT (fieldErrors).";
    private static final String BODY = """
            {
              "push": true,
              "chat": true,
              "activity": true,
              "favoriteSpace": false,
              "marketing": true,
              "dndEnabled": true,
              "dndStartHour": 22,
              "dndEndHour": 8
            }
            """;

    @MockitoBean
    private NotificationSettingService notificationSettingService;

    @Test
    @DisplayName("내 알림 설정 조회")
    void getMySettings() throws Exception {
        given(notificationSettingService.getMySettings(USER_ID)).willReturn(saved());

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dndStartHour").value(22))
                .andExpect(jsonPath("$.data.marketingConsentedAt").value("2026-10-20T21:00:00"))
                .andDo(document("notification-settings-get",
                        resource(TAG, "내 알림 설정 조회", GET_DESCRIPTION),
                        requestHeaders(authorization()),
                        responseFields(success(settingFields()))));
    }

    @Test
    @DisplayName("내 알림 설정 조회: 저장한 적이 없으면 기본값")
    void getDefaultSettings() throws Exception {
        given(notificationSettingService.getMySettings(USER_ID)).willReturn(NotificationSettingResponse.defaults());

        mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.push").value(true))
                .andExpect(jsonPath("$.data.favoriteSpace").value(false))
                .andExpect(jsonPath("$.data.marketing").value(false))
                .andExpect(jsonPath("$.data.dndEnabled").value(true))
                .andExpect(jsonPath("$.data.dndEndHour").value(8))
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist())
                .andDo(document("notification-settings-get-default",
                        resource(TAG, "내 알림 설정 조회", GET_DESCRIPTION),
                        requestHeaders(authorization()),
                        responseFields(success(settingFields()))));
    }

    @Test
    @DisplayName("토큰 없이 호출하면 401 UNAUTHORIZED")
    void getWithoutToken() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andDo(document("notification-settings-unauthorized",
                        resource(TAG, "내 알림 설정 조회", "토큰 없이 호출한 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("내 알림 설정 변경: 전체 교체 후 저장된 값을 준다")
    void replaceMySettings() throws Exception {
        given(notificationSettingService.replaceMySettings(eq(USER_ID), any(NotificationSettingRequest.class)))
                .willReturn(saved());

        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketing").value(true))
                .andDo(document("notification-settings-replace",
                        resource(TAG, "내 알림 설정 변경", PUT_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestFields(requestFieldDescriptors()),
                        responseFields(success(settingFields()))));
        then(notificationSettingService).should().replaceMySettings(USER_ID,
                new NotificationSettingRequest(true, true, true, false, true, true, 22, 8));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 24})
    @DisplayName("내 알림 설정 변경: 시각이 0~23 을 벗어나면 400 INVALID_INPUT")
    void replaceWithHourOutOfRange(int hour) throws Exception {
        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"dndStartHour\": 22", "\"dndStartHour\": " + hour)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("dndStartHour"))
                .andDo(document("notification-settings-replace-invalid-hour-" + (hour < 0 ? "negative" : "over"),
                        resource(TAG, "내 알림 설정 변경", PUT_DESCRIPTION),
                        responseFields(validationError())));
        then(notificationSettingService).should(never()).replaceMySettings(anyLong(), any());
    }

    @Test
    @DisplayName("내 알림 설정 변경: 방해금지를 켰는데 시작과 끝이 같으면 400 INVALID_INPUT")
    void replaceWithSameDndHours() throws Exception {
        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"dndEndHour\": 8", "\"dndEndHour\": 22")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("dndHoursDistinct"));
        then(notificationSettingService).should(never()).replaceMySettings(anyLong(), any());
    }

    @Test
    @DisplayName("내 알림 설정 변경: 방해금지를 껐으면 시작과 끝이 같아도 저장한다")
    void replaceWithSameHoursWhenDndDisabled() throws Exception {
        given(notificationSettingService.replaceMySettings(eq(USER_ID), any(NotificationSettingRequest.class)))
                .willReturn(saved());

        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"dndEnabled\": true", "\"dndEnabled\": false")
                                .replace("\"dndEndHour\": 8", "\"dndEndHour\": 22")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 알림 설정 변경: 항목이 빠지면 400 INVALID_INPUT")
    void replaceWithMissingField() throws Exception {
        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"marketing\": true,", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("marketing"))
                .andDo(document("notification-settings-replace-missing-field",
                        resource(TAG, "내 알림 설정 변경", PUT_DESCRIPTION),
                        responseFields(validationError())));
        then(notificationSettingService).should(never()).replaceMySettings(anyLong(), any());
    }

    private static NotificationSettingResponse saved() {
        return new NotificationSettingResponse(true, true, true, false, true, true, 22, 8,
                FixedClockConfig.FIXED_LOCAL_NOW, FixedClockConfig.FIXED_LOCAL_NOW);
    }

    private static FieldDescriptor[] requestFieldDescriptors() {
        return new FieldDescriptor[]{
                fieldWithPath("push").type(JsonFieldType.BOOLEAN).description("푸시 알림 전체. false 면 아래 분류와 관계없이 보내지 않음"),
                fieldWithPath("chat").type(JsonFieldType.BOOLEAN).description("1:1 대화 알림"),
                fieldWithPath("activity").type(JsonFieldType.BOOLEAN).description("활동 알림 (신청 · 가격 협의 · 승인 · 거절)"),
                fieldWithPath("favoriteSpace").type(JsonFieldType.BOOLEAN).description("찜한 공간 소식"),
                fieldWithPath("marketing").type(JsonFieldType.BOOLEAN).description("광고성 정보 수신 동의 (이벤트 · 혜택)"),
                fieldWithPath("dndEnabled").type(JsonFieldType.BOOLEAN).description("방해금지 사용"),
                fieldWithPath("dndStartHour").type(JsonFieldType.NUMBER).description("방해금지 시작 시각 (0~23시, 포함)"),
                fieldWithPath("dndEndHour").type(JsonFieldType.NUMBER)
                        .description("방해금지 끝 시각 (0~23시, 미포함). 시작보다 작으면 자정을 넘는 구간")
        };
    }

    private static FieldDescriptor[] settingFields() {
        return new FieldDescriptor[]{
                fieldWithPath("data.push").type(JsonFieldType.BOOLEAN).description("푸시 알림 전체"),
                fieldWithPath("data.chat").type(JsonFieldType.BOOLEAN).description("1:1 대화 알림"),
                fieldWithPath("data.activity").type(JsonFieldType.BOOLEAN).description("활동 알림 (신청 · 가격 협의 · 승인 · 거절)"),
                fieldWithPath("data.favoriteSpace").type(JsonFieldType.BOOLEAN).description("찜한 공간 소식"),
                fieldWithPath("data.marketing").type(JsonFieldType.BOOLEAN).description("광고성 정보 수신 동의"),
                fieldWithPath("data.dndEnabled").type(JsonFieldType.BOOLEAN).description("방해금지 사용"),
                fieldWithPath("data.dndStartHour").type(JsonFieldType.NUMBER).description("방해금지 시작 시각 (0~23시, 포함)"),
                fieldWithPath("data.dndEndHour").type(JsonFieldType.NUMBER).description("방해금지 끝 시각 (0~23시, 미포함)"),
                fieldWithPath("data.marketingConsentedAt").type(JsonFieldType.STRING).optional()
                        .description("광고성 정보 수신 동의 시각 (KST). 동의하지 않았으면 null — 서버가 기록"),
                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).optional()
                        .description("마지막 저장 시각 (KST). 저장한 적이 없으면 null — 기본값")
        };
    }
}
