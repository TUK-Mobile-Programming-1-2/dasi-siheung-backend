package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.support.TestFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 게시글 사진: 저장소 파일과 응답 주소, 지울 때 파일 정리를 확인한다.
 * 파일은 커밋된 뒤에 지워지므로 테스트 트랜잭션을 쓰지 않는다. (만든 회원 · 글은 테스트 DB 에 남는다)
 */
@IntegrationTest
class PostPhotoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private FileStorage fileStorage;

    @Test
    @DisplayName("글쓰기 → 사진 2장 → 상세 · 목록 썸네일 → 1장 삭제(파일도) → 글 삭제(남은 파일도)")
    void photoLifecycle() throws Exception {
        TestAccounts.Account author = testAccounts.create(Role.USER);
        String title = "사진 게시글 " + UUID.randomUUID();
        long postId = writePost(author, title);

        JsonNode photos = json(upload(author, postId, TestFiles.jpeg("files"), png("files"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2)));
        long firstId = photos.at("/0/id").asLong();
        String firstUrl = photos.at("/0/url").asText();
        String secondUrl = photos.at("/1/url").asText();
        assertThat(firstUrl).contains(LocalFileStorage.URL_PREFIX + "public/community/posts/" + postId + "/photos/")
                .endsWith(".jpg");
        assertThat(secondUrl).endsWith(".png");
        assertThat(storedFile(firstUrl)).exists();
        assertThat(storedFile(secondUrl)).exists();

        mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
                .andExpect(jsonPath("$.data.photos.length()").value(2))
                .andExpect(jsonPath("$.data.photos[0].id").value(firstId))
                .andExpect(jsonPath("$.data.photos[1].url").value(secondUrl));
        mockMvc.perform(get("/api/v1/community/posts").param("keyword", title))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value(firstUrl));

        mockMvc.perform(delete("/api/v1/community/posts/{postId}/photos/{photoId}", postId, firstId)
                        .header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isOk());
        assertThat(storedFile(firstUrl)).doesNotExist();
        mockMvc.perform(get("/api/v1/community/posts").param("keyword", title))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value(secondUrl));
        mockMvc.perform(delete("/api/v1/community/posts/{postId}/photos/{photoId}", postId, firstId)
                        .header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("POST_PHOTO_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/community/posts/{postId}", postId).header(HttpHeaders.AUTHORIZATION, author.bearer()))
                .andExpect(status().isOk());
        assertThat(storedFile(secondUrl)).doesNotExist();
    }

    @Test
    @DisplayName("사진 없는 글은 썸네일 null · 사진 빈 배열")
    void postWithoutPhotos() throws Exception {
        TestAccounts.Account author = testAccounts.create(Role.USER);
        String title = "사진 없는 글 " + UUID.randomUUID();
        long postId = writePost(author, title);

        mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
                .andExpect(jsonPath("$.data.photos.length()").value(0));
        mockMvc.perform(get("/api/v1/community/posts").param("keyword", title))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").doesNotExist());
    }

    @Test
    @DisplayName("5장을 넘기면 파일을 올리기 전에 400, 남의 글이면 403 — 둘 다 저장소에 파일이 남지 않는다")
    void limitAndAuthorChecked() throws Exception {
        TestAccounts.Account author = testAccounts.create(Role.USER);
        long postId = writePost(author, "사진 제한 " + UUID.randomUUID());
        upload(author, postId, jpegs(3)).andExpect(status().isCreated());
        List<Path> before = storedPostPhotos(postId);

        upload(author, postId, jpegs(3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("POST_PHOTO_LIMIT_EXCEEDED"));
        upload(testAccounts.create(Role.USER), postId, jpegs(1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_POST_AUTHOR"));
        assertThat(storedPostPhotos(postId)).containsExactlyInAnyOrderElementsOf(before);

        upload(author, postId, jpegs(2)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    @DisplayName("두 번째 파일이 이미지가 아니면 400 → 먼저 올린 파일도 지워지고 사진은 그대로다")
    void rollsBackOnInvalidFile() throws Exception {
        TestAccounts.Account author = testAccounts.create(Role.USER);
        long postId = writePost(author, "사진 롤백 " + UUID.randomUUID());

        upload(author, postId, TestFiles.jpeg("files"), TestFiles.fakeImage("files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));

        assertThat(storedPostPhotos(postId)).isEmpty();
        mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
                .andExpect(jsonPath("$.data.photos.length()").value(0));
    }

    private long writePost(TestAccounts.Account author, String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("category", "RECRUIT", "title", title, "content", "같이 하실 분"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photos.length()").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private ResultActions upload(TestAccounts.Account member, long postId, MockMultipartFile... files) throws Exception {
        var request = multipart("/api/v1/community/posts/{postId}/photos", postId);
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, member.bearer()));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .at("/data");
    }

    private static MockMultipartFile png(String partName) {
        return new MockMultipartFile(partName, "photo.png", "image/png", TestFiles.pngBytes());
    }

    private static MockMultipartFile[] jpegs(int count) {
        List<MockMultipartFile> files = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            files.add(TestFiles.jpeg("files"));
        }
        return files.toArray(MockMultipartFile[]::new);
    }

    private Path storedFile(String url) {
        String marker = LocalFileStorage.URL_PREFIX + "public/";
        return ((LocalFileStorage) fileStorage).root().resolve("public")
                .resolve(url.substring(url.indexOf(marker) + marker.length()));
    }

    private List<Path> storedPostPhotos(long postId) throws IOException {
        Path directory = ((LocalFileStorage) fileStorage).root().resolve("public/community/posts/" + postId + "/photos");
        if (Files.notExists(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.toList();
        }
    }
}
