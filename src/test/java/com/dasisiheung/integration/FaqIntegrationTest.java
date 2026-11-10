package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
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
class FaqIntegrationTest {

    private static final String PUBLIC = "/api/v1/faqs";
    private static final String ADMIN = "/api/v1/admin/faqs";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("토큰 없이 볼 수 있고, 분류로 거르면 그 분류만 나온다")
    void publicListAndCategoryFilter() throws Exception {
        mockMvc.perform(get(PUBLIC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get(PUBLIC).param("category", "SPACE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].category").value(everyItem(is("SPACE"))));
    }

    @Test
    @DisplayName("관리자가 추가하면 맨 뒤에 붙고, 수정 · 순서 변경 · 삭제가 공개 목록에 그대로 보인다")
    void adminManagesFaqs() throws Exception {
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);

        long created = id(create(admin, "ETC", "통합 테스트 질문", "통합 테스트 답변")
                .andExpect(status().isCreated()));
        List<Long> ids = publicIds();
        assertThat(ids.get(ids.size() - 1)).isEqualTo(created);

        mockMvc.perform(put(ADMIN + "/{faqId}", created)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category": "COMMUNITY", "question": "고친 질문", "answer": "고친 답변"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("COMMUNITY"));

        List<Long> reordered = new ArrayList<>(ids);
        reordered.remove(Long.valueOf(created));
        reordered.add(0, created);
        mockMvc.perform(put(ADMIN + "/order")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"faqIds\": " + reordered + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(created));
        clearPersistenceContext();
        assertThat(publicIds()).containsExactlyElementsOf(reordered);

        mockMvc.perform(delete(ADMIN + "/{faqId}", created).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk());
        assertThat(publicIds()).doesNotContain(created);
        mockMvc.perform(delete(ADMIN + "/{faqId}", created).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FAQ_NOT_FOUND"));
    }

    @Test
    @DisplayName("순서 변경에 빠진 ID 가 있으면 400 INVALID_FAQ_ORDER 이고 순서는 그대로다")
    void reorderWithMissingId() throws Exception {
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        List<Long> before = publicIds();

        mockMvc.perform(put(ADMIN + "/order")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"faqIds\": " + before.subList(1, before.size()) + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FAQ_ORDER"));

        clearPersistenceContext();
        assertThat(publicIds()).containsExactlyElementsOf(before);
    }

    @Test
    @DisplayName("이용자 · 공간 제공자는 관리할 수 없다 (403)")
    void onlyAdminCanManage() throws Exception {
        for (Role role : List.of(Role.USER, Role.PROVIDER)) {
            create(testAccounts.create(role), "ETC", "질문", "답변")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    private ResultActions create(TestAccounts.Account account, String category, String question, String answer)
            throws Exception {
        return mockMvc.perform(post(ADMIN)
                .header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"category": "%s", "question": "%s", "answer": "%s"}
                        """.formatted(category, question, answer)));
    }

    private List<Long> publicIds() throws Exception {
        String body = mockMvc.perform(get(PUBLIC)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$.data[*].id");
        return ids.stream().map(Number::longValue).toList();
    }

    private static long id(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
