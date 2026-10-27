package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Test
    @DisplayName("내 정보를 수정하고 다시 조회하면 바뀐 값이 보인다")
    void updateAndGet() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.USER);

        mockMvc.perform(patch("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone": "010-5555-6666"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("01055556666"))
                .andExpect(jsonPath("$.data.email").value(account.email()));
    }

    @Test
    @DisplayName("탈퇴하면 같은 이메일·비밀번호로 로그인할 수 없다")
    void withdrawThenCannotLogin() throws Exception {
        TestAccounts.Account account = testAccounts.create(Role.PROVIDER);

        mockMvc.perform(post("/api/v1/members/me/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "%s"}
                                """.formatted(TestAccounts.PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(account.email(), TestAccounts.PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }
}
