package com.dasisiheung.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class CommunityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Test
    @DisplayName("글쓰기 → 검색 → 상세(조회 수) → 댓글 2개 → 남의 댓글 삭제 403 → 관리자 삭제 → 수정 → 삭제")
    void communityFlow() throws Exception {
        TestAccounts.Account writer = testAccounts.create(Role.USER);
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        String token = "모집" + UUID.randomUUID().toString().substring(0, 8);

        String body = mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, writer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"RECRUIT\", \"title\": \"" + token + " 팀원\", \"content\": \"함께해요 100%\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mine").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long postId = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(get("/api/v1/community/posts").param("keyword", token).param("category", "RECRUIT"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/community/posts").param("keyword", token).param("category", "QUESTION"))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/community/posts/{id}", postId))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.mine").value(false));

        long commentId = writeComment(provider, postId, "저도 참여하고 싶어요");
        writeComment(writer, postId, "환영합니다");
        mockMvc.perform(get("/api/v1/community/posts/{id}/comments", postId))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].author.role").value("PROVIDER"));

        mockMvc.perform(delete("/api/v1/community/comments/{id}", commentId).header(HttpHeaders.AUTHORIZATION, writer.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_COMMENT_AUTHOR"));
        mockMvc.perform(delete("/api/v1/community/comments/{id}", commentId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/community/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, writer.bearer()))
                .andExpect(jsonPath("$.data.commentCount").value(1))
                .andExpect(jsonPath("$.data.mine").value(true));

        mockMvc.perform(put("/api/v1/community/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, provider.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"RECRUIT\", \"title\": \"가로채기\", \"content\": \"남의 글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_POST_AUTHOR"));
        mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, writer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"NOTICE\", \"title\": \"공지\", \"content\": \"일반 회원 공지\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOTICE_ADMIN_ONLY"));

        mockMvc.perform(delete("/api/v1/community/posts/{id}", postId).header(HttpHeaders.AUTHORIZATION, writer.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/community/posts/{id}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    @DisplayName("검색어의 % 는 글자 그대로 찾는다")
    void keywordEscapesWildcards() throws Exception {
        TestAccounts.Account writer = testAccounts.create(Role.USER);
        String token = "할인" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, writer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"LOCAL_NEWS\", \"title\": \"" + token + " 50% 행사\", \"content\": \"내용\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/community/posts").param("keyword", token + " 50%"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/community/posts").param("keyword", token + " 5_%"))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    private long writeComment(TestAccounts.Account author, long postId, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts/{id}/comments", postId)
                        .header(HttpHeaders.AUTHORIZATION, author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}
