package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class InquiryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private FileStorage fileStorage;

    @Test
    @DisplayName("등록(답변 대기) → 사진 2장 → 4장째 400 → 관리자 답변 → 내 상세에 답변 · 시각, 다시 답변 409, 사진 409")
    void inquiryFlow() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);

        long inquiryId = idOf(create(user, "ACCOUNT", "  비밀번호를 바꿨는데 로그인이 안 돼요.  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.content").value("비밀번호를 바꿨는데 로그인이 안 돼요."))
                .andExpect(jsonPath("$.data.answer").value(nullValue()))
                .andExpect(jsonPath("$.data.photoCount").value(0))
                .andExpect(jsonPath("$.data.photos").isEmpty()));

        uploadPhotos(user, inquiryId, TestFiles.jpeg("files"),
                new MockMultipartFile("files", "photo.png", "image/png", TestFiles.pngBytes()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photoCount").value(2))
                .andExpect(jsonPath("$.data.photos[0].url").value(containsString("/local-storage/private/inquiries/")));
        assertThat(storedPhotos(inquiryId)).hasSize(2);

        uploadPhotos(user, inquiryId, TestFiles.jpeg("files"), TestFiles.jpeg("files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_PHOTO_LIMIT_EXCEEDED"));
        assertThat(storedPhotos(inquiryId)).hasSize(2);

        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.pendingInquiryCount").value(greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/api/v1/admin/inquiries").param("status", "WAITING").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == %d)].photoCount".formatted(inquiryId)).value(contains(2)))
                .andExpect(jsonPath("$.data.content[?(@.id == %d)].memberRole".formatted(inquiryId)).value(contains("USER")))
                .andExpect(jsonPath("$.data.content[?(@.id == %d)].photos".formatted(inquiryId)).value(contains(nullValue())));
        mockMvc.perform(get("/api/v1/admin/inquiries/{id}", inquiryId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(user.id()))
                .andExpect(jsonPath("$.data.photos.length()").value(2));

        answer(admin, inquiryId, "  앱을 다시 실행해 주세요.  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ANSWERED"))
                .andExpect(jsonPath("$.data.answer").value("앱을 다시 실행해 주세요."))
                .andExpect(jsonPath("$.data.answeredAt").isNotEmpty());
        answer(admin, inquiryId, "두 번째 답변")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_ALREADY_ANSWERED"));

        mockMvc.perform(get("/api/v1/inquiries/{id}", inquiryId).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ANSWERED"))
                .andExpect(jsonPath("$.data.answer").value("앱을 다시 실행해 주세요."))
                .andExpect(jsonPath("$.data.answeredAt").isNotEmpty())
                .andExpect(jsonPath("$.data.photos.length()").value(2));
        uploadPhotos(user, inquiryId, TestFiles.jpeg("files"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_ALREADY_ANSWERED"));
    }

    @Test
    @DisplayName("내 문의 내역: 내 것만 최근 순, 공간 제공자도 쓴다. 남의 문의는 403, 없는 문의는 404")
    void myInquiriesAndOwnership() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);

        long first = idOf(create(user, "PAYMENT", "환불은 어떻게 받나요?").andExpect(status().isCreated()));
        long second = idOf(create(user, "ETC", "기타 문의").andExpect(status().isCreated()));
        long providerInquiry = idOf(create(provider, "SPACE", "공간 정보를 고치고 싶어요.").andExpect(status().isCreated()));
        uploadPhotos(user, first, TestFiles.jpeg("files")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/members/me/inquiries").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(second))
                .andExpect(jsonPath("$.data.content[0].photoCount").value(0))
                .andExpect(jsonPath("$.data.content[1].id").value(first))
                .andExpect(jsonPath("$.data.content[1].category").value("PAYMENT"))
                .andExpect(jsonPath("$.data.content[1].photoCount").value(1))
                .andExpect(jsonPath("$.data.content[1].photos").value(nullValue()));
        mockMvc.perform(get("/api/v1/members/me/inquiries").header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(providerInquiry));

        mockMvc.perform(get("/api/v1/inquiries/{id}", first).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_INQUIRY_OWNER"));
        uploadPhotos(provider, first, TestFiles.jpeg("files"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_INQUIRY_OWNER"));
        mockMvc.perform(get("/api/v1/inquiries/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INQUIRY_NOT_FOUND"));
    }

    @Test
    @DisplayName("1000자는 등록되고 1001자 · 빈 내용 · 없는 분류는 400 INVALID_INPUT")
    void contentLength() throws Exception {
        TestAccounts.Account user = testAccounts.create(Role.USER);

        create(user, "ACCOUNT", "가".repeat(1000)).andExpect(status().isCreated());
        create(user, "ACCOUNT", "가".repeat(1001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("content"));
        create(user, "ACCOUNT", "   ").andExpect(status().isBadRequest());
        create(user, "NOT_A_CATEGORY", "내용").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private ResultActions create(TestAccounts.Account account, String category, String content) throws Exception {
        String body = objectMapper.createObjectNode().put("category", category).put("content", content).toString();
        return mockMvc.perform(post("/api/v1/inquiries").header(HttpHeaders.AUTHORIZATION, account.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions uploadPhotos(TestAccounts.Account account, long inquiryId, MockMultipartFile... files)
            throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/inquiries/{id}/photos", inquiryId);
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, account.bearer()));
    }

    private ResultActions answer(TestAccounts.Account admin, long inquiryId, String answer) throws Exception {
        String body = objectMapper.createObjectNode().put("answer", answer).toString();
        return mockMvc.perform(post("/api/v1/admin/inquiries/{id}/answer", inquiryId)
                .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 로컬 저장소(build/test-storage)에 저장된 이 문의의 사진. */
    private Set<Path> storedPhotos(long inquiryId) throws IOException {
        Path directory = ((LocalFileStorage) fileStorage).root().resolve("private/inquiries/" + inquiryId);
        if (Files.notExists(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).collect(Collectors.toSet());
        }
    }

    private long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}
