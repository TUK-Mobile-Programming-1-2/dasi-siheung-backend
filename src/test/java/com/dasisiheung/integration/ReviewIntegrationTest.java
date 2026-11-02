package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.BookingScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ReviewIntegrationTest {

    private static final LocalDate DAY1 = LocalDate.now(ClockConfig.SERVICE_ZONE).plusDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private BookingScenario bookingScenario;

    @Test
    @DisplayName("이용 전 409 → 이용 후 작성 → 중복 409 → 평균 반영 → 수정 · 답글 → 삭제하면 평점 없음")
    void reviewFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long applicationId = bookingScenario.approvedBooking(provider, testAccounts.create(Role.ADMIN), user,
                DAY1, DAY1.plusDays(1));

        write(user, applicationId, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ALLOWED"));

        bookingScenario.movePeriod(applicationId, -3, -2);
        String body = write(user, applicationId, 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long reviewId = objectMapper.readTree(body).at("/data/id").asLong();
        long spaceId = objectMapper.readTree(body).at("/data/spaceId").asLong();
        write(user, applicationId, 4)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REVIEW"));

        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.ratingAverage").value(5.0));
        mockMvc.perform(get("/api/v1/spaces").param("sort", "RATING").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].ratingAverage").exists());

        mockMvc.perform(put("/api/v1/reviews/{id}", reviewId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 3, \"content\": \"주차가 조금 불편했어요\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reviews/{id}/reply", reviewId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"다음엔 주차 안내를 드릴게요\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply").value("다음엔 주차 안내를 드릴게요"));
        mockMvc.perform(get("/api/v1/spaces/{id}/reviews", spaceId))
                .andExpect(jsonPath("$.data.content[0].rating").value(3))
                .andExpect(jsonPath("$.data.content[0].reply").exists());
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.ratingAverage").value(3.0));

        mockMvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .header(HttpHeaders.AUTHORIZATION, testAccounts.create(Role.USER).bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_REVIEW_OWNER"));
        mockMvc.perform(delete("/api/v1/reviews/{id}", reviewId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId))
                .andExpect(jsonPath("$.data.reviewCount").value(0))
                .andExpect(jsonPath("$.data.ratingAverage").doesNotExist());
    }

    private ResultActions write(TestAccounts.Account user, long applicationId, int rating) throws Exception {
        return mockMvc.perform(post("/api/v1/applications/{id}/review", applicationId)
                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\": %d, \"content\": \"좋은 공간이었어요\"}".formatted(rating)));
    }
}
