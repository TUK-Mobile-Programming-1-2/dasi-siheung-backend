package com.dasisiheung.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 통합 테스트에서 실제 API 로 신청서를 내는 도우미. */
@TestComponent
@RequiredArgsConstructor
public class ApplicationScenario {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    /** @return 신청 ID */
    public long submit(TestAccounts.Account applicant, long spaceId, LocalDate startDate, LocalDate endDate)
            throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                        .header(HttpHeaders.AUTHORIZATION, applicant.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ApplicationFixtures.request(spaceId, startDate, endDate))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}
