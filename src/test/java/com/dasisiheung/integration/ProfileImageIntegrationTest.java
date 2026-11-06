package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.LocalFileStorage;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.support.TestFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 프로필 사진: 저장소 파일과 응답 주소가 맞는지 확인한다.
 * 이전 사진은 커밋된 뒤에 지워지므로 테스트 트랜잭션을 쓰지 않는다. (만든 회원 · 글은 테스트 DB 에 남는다)
 */
@IntegrationTest
class ProfileImageIntegrationTest {

    private static final String PROFILE_URL_PART = LocalFileStorage.URL_PREFIX + "public/members/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private SpaceScenario spaceScenario;

    @Autowired
    private FileStorage fileStorage;

    @Test
    @DisplayName("올린 사진이 내 정보 · 글쓴이 · 대화 상대에 보이고, 바꾸면 이전 파일이, 지우면 마지막 파일이 지워진다")
    void uploadReplaceAndDelete() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account user = testAccounts.create(Role.USER);

        String first = profileImageUrl(upload(provider, TestFiles.jpeg("image")).andExpect(status().isOk()));
        assertThat(first).contains(PROFILE_URL_PART + provider.id() + "/profile/").endsWith(".jpg");
        assertThat(storedFile(first)).exists();

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(first));

        long postId = writePost(provider);
        mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
                .andExpect(jsonPath("$.data.author.profileImageUrl").value(first));
        mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", postId)
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"사진 잘 보여요\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.author.id").value(user.id()))
                .andExpect(jsonPath("$.data.author.profileImageUrl").doesNotExist());

        long spaceId = spaceScenario.publishedSpace(provider, testAccounts.create(Role.ADMIN), Set.of());
        mockMvc.perform(post("/api/v1/spaces/{spaceId}/conversations", spaceId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.counterpart.id").value(provider.id()))
                .andExpect(jsonPath("$.data.counterpart.profileImageUrl").value(first));

        MockMultipartFile png = new MockMultipartFile("image", "profile.png", "image/png", TestFiles.pngBytes());
        String second = profileImageUrl(upload(provider, png).andExpect(status().isOk()));
        assertThat(second).isNotEqualTo(first).endsWith(".png");
        assertThat(storedFile(second)).exists();
        assertThat(storedFile(first)).doesNotExist();

        mockMvc.perform(delete("/api/v1/members/me/profile-image").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());
        assertThat(storedFile(second)).doesNotExist();
        mockMvc.perform(delete("/api/v1/members/me/profile-image").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 400 UNSUPPORTED_FILE_TYPE, 기존 사진은 그대로다")
    void rejectsFakeImageAndKeepsCurrent() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);
        String current = profileImageUrl(upload(user, TestFiles.jpeg("image")));

        upload(user, TestFiles.fakeImage("image"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));

        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.profileImageUrl").value(current));
        assertThat(storedFile(current)).exists();
    }

    @Test
    @DisplayName("탈퇴하면 프로필 사진 파일이 지워지고, 남은 글의 글쓴이 사진도 null 이다")
    void withdrawDeletesProfileImage() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);
        String url = profileImageUrl(upload(user, TestFiles.jpeg("image")));
        long postId = writePost(user);

        mockMvc.perform(post("/api/v1/members/me/withdraw").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + TestAccounts.PASSWORD + "\"}"))
                .andExpect(status().isOk());

        assertThat(storedFile(url)).doesNotExist();
        mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
                .andExpect(jsonPath("$.data.author.name").value("탈퇴한 회원"))
                .andExpect(jsonPath("$.data.author.profileImageUrl").doesNotExist());
    }

    @Test
    @DisplayName("토큰 없이 올리면 401")
    void requiresLogin() throws Exception {
        mockMvc.perform(multipart("/api/v1/members/me/profile-image").file(TestFiles.jpeg("image")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value(containsString("UNAUTHORIZED")));
    }

    private ResultActions upload(TestAccounts.Account member, MockMultipartFile image) throws Exception {
        return mockMvc.perform(multipart("/api/v1/members/me/profile-image").file(image)
                .header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }

    private String profileImageUrl(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/profileImageUrl").asText();
    }

    private long writePost(TestAccounts.Account author) throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"QUESTION\", \"title\": \"프로필 사진 테스트\", \"content\": \"본문\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private Path storedFile(String url) {
        String marker = LocalFileStorage.URL_PREFIX + "public/";
        return ((LocalFileStorage) fileStorage).root().resolve("public")
                .resolve(url.substring(url.indexOf(marker) + marker.length()));
    }
}
