package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 DB 로 가입 → 로그인 → 재발급(교체) → 로그아웃 흐름을 검증한다. 테스트 데이터는 롤백된다.
 */
@IntegrationTest
@Transactional
class AuthIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("가입 → 로그인 → 재발급하면 이전 리프레시 토큰은 쓸 수 없고, 로그아웃하면 새 토큰도 폐기된다")
    void fullTokenLifecycle() throws Exception {
        String email = uniqueEmail();
        signup(email, "PROVIDER");

        JsonNode login = postJson("/api/v1/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(email.toUpperCase(), PASSWORD));
        String firstRefreshToken = login.at("/data/refreshToken").asText();

        JsonNode refreshed = postJson("/api/v1/auth/refresh", refreshBody(firstRefreshToken));
        String secondRefreshToken = refreshed.at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(secondRefreshToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(secondRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("같은 이메일(대소문자만 다름)로 다시 가입하면 409")
    void duplicateEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        signup(email, "USER");

        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email.toUpperCase(), "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("관리자 역할로 가입하면 400 ROLE_NOT_ALLOWED")
    void adminSignupRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(uniqueEmail(), "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("위조된 토큰으로 보호된 API 를 호출하면 401 INVALID_TOKEN")
    void forgedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("공개 API 는 잘못된 토큰이 붙어 있어도 그대로 호출된다")
    void publicApiIgnoresBadToken() throws Exception {
        mockMvc.perform(get("/api/v1/system/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
                .andExpect(status().isOk());
    }

    private void signup(String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email, role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value(role));
    }

    private JsonNode postJson(String url, String body) throws Exception {
        String response = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private static String signupBody(String email, String role) {
        return """
                {"email": "%s", "password": "%s", "name": "통합테스트", "phone": "010-1234-5678", "role": "%s"}
                """.formatted(email, PASSWORD, role);
    }

    private static String refreshBody(String refreshToken) {
        return """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
    }

    private static String uniqueEmail() {
        return "it-" + UUID.randomUUID() + "@test.dasisiheung.com";
    }
}
