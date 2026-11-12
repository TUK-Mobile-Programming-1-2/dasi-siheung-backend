package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.ApiDocumentation.validationError;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.service.PushTokenService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(PushTokenController.class)
class PushTokenControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "PushToken";
    private static final String PATH = "/api/v1/members/me/push-tokens";
    private static final String TOKEN = "dQw4w9WgXcQ:APA91bH-example-fcm-registration-token";
    private static final String REGISTER_DESCRIPTION = "앱 시작 · 로그인 직후 · FirebaseMessagingService.onNewToken 때 호출합니다. "
            + "같은 토큰을 여러 번 보내도 되고, 다른 계정이 쓰던 토큰이면 지금 로그인한 계정으로 옮겨집니다(한 기기 = 마지막 로그인 계정). "
            + "60일 넘게 다시 등록되지 않은 토큰은 서버가 지웁니다.";

    @MockitoBean
    private PushTokenService pushTokenService;

    @Test
    @DisplayName("기기 토큰 등록")
    void register() throws Exception {
        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + TOKEN + "\"}"))
                .andExpect(status().isOk())
                .andDo(document("push-token-register",
                        resource(TAG, "기기 토큰 등록", REGISTER_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestFields(fieldWithPath("token").type(JsonFieldType.STRING)
                                .description("FCM 등록 토큰 (최대 512자)")),
                        responseFields(success())));
        then(pushTokenService).should().register(USER_ID, TOKEN);
    }

    @Test
    @DisplayName("기기 토큰 등록: 토큰이 비었으면 400 INVALID_INPUT")
    void registerBlank() throws Exception {
        mockMvc.perform(put(PATH)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("token"))
                .andDo(document("push-token-register-invalid",
                        resource(TAG, "기기 토큰 등록", REGISTER_DESCRIPTION),
                        responseFields(validationError())));
        then(pushTokenService).should(never()).register(anyLong(), anyString());
    }

    @Test
    @DisplayName("토큰 없이 호출하면 401 UNAUTHORIZED")
    void registerWithoutLogin() throws Exception {
        mockMvc.perform(put(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + TOKEN + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andDo(document("push-token-unauthorized",
                        resource(TAG, "기기 토큰 등록", "로그인 없이 호출한 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("기기 토큰 해제")
    void unregister() throws Exception {
        mockMvc.perform(delete(PATH + "?token={token}", TOKEN).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isOk())
                .andDo(document("push-token-unregister",
                        resource(TAG, "기기 토큰 해제", "로그아웃 API 를 부르기 전에 호출합니다. "
                                + "내 토큰이 아니거나 이미 없어도 200 입니다."),
                        requestHeaders(authorization()),
                        queryParameters(parameterWithName("token").description("해제할 FCM 등록 토큰")),
                        responseFields(success())));
        then(pushTokenService).should().unregister(USER_ID, TOKEN);
    }

    @Test
    @DisplayName("기기 토큰 해제: token 이 없으면 400 INVALID_INPUT")
    void unregisterWithoutToken() throws Exception {
        mockMvc.perform(delete(PATH).header(HttpHeaders.AUTHORIZATION, userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        then(pushTokenService).should(never()).unregister(anyLong(), anyString());
    }
}
