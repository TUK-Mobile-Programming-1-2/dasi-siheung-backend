package com.dasisiheung.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.space.SpaceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;

/** 통합 테스트에서 실제 API 로 공간을 등록 → 증빙 → 관리자 승인까지 진행하는 도우미. */
@TestComponent
@RequiredArgsConstructor
public class SpaceScenario {

    private static final String APPROVE_ALL = """
            {"decision": "APPROVE", "checklist": {"authorityVerified": true, "addressMatched": true,
             "buildingUnitMatched": true, "areaMatched": true, "usageMatched": true}}
            """;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public long register(TestAccounts.Account provider, Set<LocalDate> availableDates) throws Exception {
        return register(provider, SpaceFixtures.request(availableDates));
    }

    public long register(TestAccounts.Account provider, SpaceRequest request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    public void addProof(TestAccounts.Account provider, long spaceId) throws Exception {
        mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/proofs", spaceId)
                        .file(TestFiles.pdf("file"))
                        .part(new MockPart("proofType", "LEASE_CONTRACT".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isCreated());
    }

    public void approve(TestAccounts.Account admin, long spaceId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/spaces/{spaceId}/reviews", spaceId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVE_ALL))
                .andExpect(status().isOk());
    }

    /** 등록 → 증빙 → 승인까지 마친 공개 공간 ID. */
    public long publishedSpace(TestAccounts.Account provider, TestAccounts.Account admin,
                               Set<LocalDate> availableDates) throws Exception {
        return publishedSpace(provider, admin, SpaceFixtures.request(availableDates));
    }

    public long publishedSpace(TestAccounts.Account provider, TestAccounts.Account admin,
                               SpaceRequest request) throws Exception {
        long spaceId = register(provider, request);
        addProof(provider, spaceId);
        approve(admin, spaceId);
        return spaceId;
    }
}
