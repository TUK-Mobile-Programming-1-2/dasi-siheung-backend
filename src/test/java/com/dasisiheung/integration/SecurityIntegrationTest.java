package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SecurityIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("헬스 체크는 인증 없이 호출되고, DB 연결까지 포함해 UP 이다")
    void healthCheckIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("ping API 는 인증 없이 호출할 수 있다")
    void pingIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/system/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OK"));
    }

    @Test
    @DisplayName("Swagger UI 와 두 가지 OpenAPI 명세는 인증 없이 조회할 수 있다")
    void apiDocumentsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
        mockMvc.perform(get("/docs/openapi3.yaml"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증 없이 보호된 API 를 호출하면 401 과 공통 오류 형식을 반환한다")
    void protectedApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("인증된 사용자가 없는 API 를 호출하면 404 를 반환한다")
    void unknownApiReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("허용된 출처의 CORS 사전 요청은 통과한다")
    void allowsPreflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/system/ping")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("허용되지 않은 출처의 CORS 사전 요청은 거부한다")
    void rejectsPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/system/ping")
                        .header(HttpHeaders.ORIGIN, "https://unknown.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
