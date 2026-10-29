package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 실제 DB 의 group by 집계(JPQL 생성자 표현식)가 동작하고, 새 공간이 수치에 반영되는지 확인한다. */
@IntegrationTest
@Transactional
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Test
    @DisplayName("공간을 등록·승인하면 검수 대기·공개 수치가 1씩 늘어난다")
    void countsReflectNewSpaces() throws Exception {
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        JsonNode before = dashboard(admin);

        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        spaceScenario.register(provider, Set.of());
        spaceScenario.publishedSpace(provider, admin, Set.of());
        JsonNode after = dashboard(admin);

        assertThat(after.at("/pendingReviewCount").asLong()).isEqualTo(before.at("/pendingReviewCount").asLong() + 1);
        assertThat(after.at("/spacesByStatus/PUBLISHED").asLong())
                .isEqualTo(before.at("/spacesByStatus/PUBLISHED").asLong() + 1);
        assertThat(after.at("/publishedSpacesByRegion/JEONGWANG").asLong())
                .isEqualTo(before.at("/publishedSpacesByRegion/JEONGWANG").asLong() + 1);
        assertThat(after.at("/membersByRole/PROVIDER").asLong())
                .isEqualTo(before.at("/membersByRole/PROVIDER").asLong() + 1);
        assertThat(after.at("/recentSpaces").size()).isLessThanOrEqualTo(5);
    }

    private JsonNode dashboard(TestAccounts.Account admin) throws Exception {
        String body = mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data");
    }
}
