package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 없이 공간을 둘러보는 흐름. */
@IntegrationTest
@Transactional
class PublicSpaceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ClockConfig.SERVICE_ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("공개된 공간은 토큰 없이 상세를 볼 수 있고, 일시 중단하면 404 가 된다")
    void detail() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of(TODAY.plusDays(2), TODAY.plusDays(1)));

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(spaceId))
                .andExpect(jsonPath("$.data.availableDates[0]").value(TODAY.plusDays(1).toString()))
                .andExpect(jsonPath("$.data.provider.id").value(provider.id()))
                .andExpect(jsonPath("$.data.proofs").doesNotExist())
                .andExpect(jsonPath("$.data.reviewComment").doesNotExist());

        mockMvc.perform(post("/api/v1/provider/spaces/{id}/pause", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("검수 대기 공간은 이용자에게 보이지 않는다")
    void pendingSpaceHidden() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = spaceScenario.register(provider, Set.of());

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("공간 둘러보기는 조회만 공개다 — 쓰기 요청은 401")
    void writeRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/spaces/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }
}
