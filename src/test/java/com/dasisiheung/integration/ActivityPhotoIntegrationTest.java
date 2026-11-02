package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.config.ClockConfig;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.BookingScenario;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.support.TestFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 저장소(build/test-storage)에 실제로 파일을 올린다. */
@IntegrationTest
@Transactional
class ActivityPhotoIntegrationTest {

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
    @DisplayName("내일 시작하는 예약: 오늘 이용 전 사진은 되고 이용 후 사진은 409, 양쪽이 함께 보고 본인 사진만 지운다")
    void photoFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);
        long applicationId = bookingScenario.approvedBooking(provider, testAccounts.create(Role.ADMIN), user,
                DAY1, DAY1);

        String body = upload(user, applicationId, "BEFORE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].uploaderSide").value("APPLICANT"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long userPhotoId = objectMapper.readTree(body).at("/data/0/id").asLong();
        upload(provider, applicationId, "BEFORE").andExpect(status().isCreated());
        upload(user, applicationId, "AFTER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACTIVITY_PHOTO_NOT_ALLOWED"));

        mockMvc.perform(get("/api/v1/applications/{id}/activity-photos", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].uploaderSide").value("PROVIDER"));
        mockMvc.perform(get("/api/v1/applications/{id}/activity-photos", applicationId)
                        .header(HttpHeaders.AUTHORIZATION, testAccounts.create(Role.USER).bearer()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/applications/{id}/activity-photos/{photoId}", applicationId, userPhotoId)
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_PHOTO_UPLOADER"));
        mockMvc.perform(delete("/api/v1/applications/{id}/activity-photos/{photoId}", applicationId, userPhotoId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk());
    }

    private ResultActions upload(TestAccounts.Account member, long applicationId, String phase) throws Exception {
        return mockMvc.perform(multipart("/api/v1/applications/{id}/activity-photos", applicationId)
                .file(TestFiles.jpeg("files"))
                .part(new MockPart("phase", phase.getBytes(StandardCharsets.UTF_8)))
                .header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }
}
