package com.dasisiheung.controller;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.support.ApiDocsTest;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@ApiDocsTest(SystemController.class)
class SystemControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ping: 인증 없이 서버 상태와 서울 기준 서버 시각을 반환한다")
    void ping() throws Exception {
        FieldDescriptor[] pingFields = {
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                fieldWithPath("data.status").type(JsonFieldType.STRING).description("서버 상태 (정상이면 OK)"),
                fieldWithPath("data.serverTime").type(JsonFieldType.STRING).description("서버 시각 (ISO-8601, Asia/Seoul)")
        };

        mockMvc.perform(get("/api/v1/system/ping").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OK"))
                .andExpect(jsonPath("$.data.serverTime").value("2026-10-20T21:00:00+09:00"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andDo(document("system-ping",
                        resource(ResourceSnippetParameters.builder()
                                .tag("System")
                                .summary("서버 연결 확인")
                                .description("앱·관리자 웹이 백엔드와 통신할 수 있는지 확인합니다. 인증 없이 호출할 수 있습니다.")
                                .responseFields(pingFields)
                                .build()),
                        responseFields(pingFields)));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(FIXED_NOW, ClockConfig.SERVICE_ZONE);
        }
    }
}
