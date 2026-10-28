package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.LocalFileStorage;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceFixtures;
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.support.TestFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 저장소(build/test-storage)에 실제로 파일을 쓰고, 테스트가 롤백되면 파일도 지워진다. */
@IntegrationTest
@Transactional
class SpaceFileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private FileStorage fileStorage;

    @Test
    @DisplayName("사진 2장 · 증빙 1개를 올리면 저장소에 파일이 생기고 공개 주소 · 임시 주소를 돌려준다")
    void uploadPhotosAndProof() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = registerSpace(provider);

        String photoBody = mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/photos", spaceId)
                        .file(TestFiles.jpeg("files"))
                        .file(TestFiles.jpeg("files"))
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sortOrder").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String photoUrl = objectMapper.readTree(photoBody).at("/data/0/url").asText();
        assertThat(photoUrl).contains("/local-storage/public/spaces/" + spaceId + "/photos/");
        assertThat(storedFile("public", photoUrl)).exists();

        mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/proofs", spaceId)
                        .file(TestFiles.pdf("file"))
                        .part(new MockPart("proofType", "LEASE_CONTRACT".getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proofType").value("LEASE_CONTRACT"))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.containsString("/local-storage/private/")));
    }

    @Test
    @DisplayName("확장자만 이미지인 파일은 400 UNSUPPORTED_FILE_TYPE")
    void rejectsFakeImage() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        long spaceId = registerSpace(provider);

        mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/photos", spaceId)
                        .file(TestFiles.fakeImage("files"))
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    @DisplayName("다른 제공자의 공간에는 사진을 올릴 수 없다 (403)")
    void otherProviderForbidden() throws Exception {
        long spaceId = registerSpace(testAccounts.create(Role.PROVIDER));
        TestAccounts.Account other = testAccounts.create(Role.PROVIDER);

        mockMvc.perform(multipart("/api/v1/provider/spaces/{spaceId}/photos", spaceId)
                        .file(TestFiles.jpeg("files"))
                        .header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_SPACE_OWNER"));
    }

    private long registerSpace(TestAccounts.Account provider) throws Exception {
        String body = mockMvc.perform(post("/api/v1/provider/spaces")
                        .header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SpaceFixtures.request(Set.of()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = objectMapper.readTree(body);
        return json.at("/data/id").asLong();
    }

    private Path storedFile(String bucketDirectory, String url) {
        String relative = url.substring(url.indexOf(LocalFileStorage.URL_PREFIX + bucketDirectory + "/")
                + (LocalFileStorage.URL_PREFIX + bucketDirectory + "/").length());
        return ((LocalFileStorage) fileStorage).root().resolve(bucketDirectory).resolve(relative);
    }
}
