package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.SpaceScenario;
import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.LocalFileStorage;
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.support.TestFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ReportIntegrationTest {

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
    @DisplayName("공간 신고 2건 → 중복 409 · 자기 신고 400 → 숨김 + 정지 → 둘 다 닫힘, 공간 404, 제공자 로그인 403 → 해제")
    void spaceReportFlow() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        TestAccounts.Account reporterA = testAccounts.create(Role.USER);
        TestAccounts.Account reporterB = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of());

        long reportId = report(reporterA, "SPACE", spaceId).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        long otherReportId = report(reporterB, "SPACE", spaceId).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        report(reporterA, "SPACE", spaceId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_REPORT"));
        report(provider, "SPACE", spaceId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_REPORT_SELF"));

        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.pendingReportCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"HIDE_AND_SUSPEND\", \"suspendDays\": 7, \"note\": \"허위 사진 확인\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.targetVisible").value(false))
                .andExpect(jsonPath("$.data.ownerStatus").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/admin/reports/{id}", otherReportId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
        mockMvc.perform(get("/api/v1/spaces/{id}", spaceId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/provider/spaces/{id}", spaceId).header(HttpHeaders.AUTHORIZATION, provider.bearer()))
                .andExpect(jsonPath("$.data.status").value("HIDDEN"))
                .andExpect(jsonPath("$.data.reviewComment").value("허위 사진 확인"));
        login(provider).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("ACCOUNT_SUSPENDED"));

        mockMvc.perform(post("/api/v1/admin/members/{id}/unsuspend", provider.id()).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        login(provider).andExpect(status().isOk());
    }

    @Test
    @DisplayName("댓글 신고를 숨김 처리하면 댓글 목록에서 빠지고 댓글 수가 줄어든다")
    void commentReport() throws Exception {
        TestAccounts.Account writer = testAccounts.create(Role.USER);
        TestAccounts.Account commenter = testAccounts.create(Role.USER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        long postId = mockMvc.perform(post("/api/v1/community/posts").header(HttpHeaders.AUTHORIZATION, writer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"QUESTION\", \"title\": \"질문\", \"content\": \"내용\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        long commentId = mockMvc.perform(post("/api/v1/community/posts/{id}/comments", postId)
                        .header(HttpHeaders.AUTHORIZATION, commenter.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"광고 댓글\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);

        long reportId = report(writer, "COMMENT", commentId).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"HIDE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/community/posts/{id}/comments", postId))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/community/posts/{id}", postId))
                .andExpect(jsonPath("$.data.commentCount").value(0));
    }

    @Test
    @DisplayName("증거 사진 2장: 비공개 저장소에 저장 → 관리자 목록은 장수만, 상세 · 처리 응답은 임시 주소")
    void reportWithPhotos() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        TestAccounts.Account reporter = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of());

        String body = report(reporter, "SPACE", spaceId, TestFiles.jpeg("photos"),
                new MockMultipartFile("photos", "photo.png", "image/png", TestFiles.pngBytes()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photos.length()").value(2))
                .andExpect(jsonPath("$.data.photos[0].url").value(containsString("/local-storage/private/reports/")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long reportId = idOf(body);
        assertThat(storedReportPhotos(reportId)).hasSize(2);

        mockMvc.perform(get("/api/v1/admin/reports").param("status", "PENDING").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.content[?(@.id == %d)].photoCount".formatted(reportId)).value(contains(2)))
                .andExpect(jsonPath("$.data.content[?(@.id == %d)].photos".formatted(reportId)).value(contains(nullValue())));
        mockMvc.perform(get("/api/v1/admin/reports/{id}", reportId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(jsonPath("$.data.photoCount").value(2))
                .andExpect(jsonPath("$.data.photos.length()").value(2))
                .andExpect(jsonPath("$.data.photos[1].url").value(containsString("/reports/" + reportId + "/photos/")));
        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"DISMISS\"}"))
                .andExpect(jsonPath("$.data.status").value("DISMISSED"))
                .andExpect(jsonPath("$.data.photos.length()").value(2));
    }

    @Test
    @DisplayName("내 신고 내역: 내 것만 최근 순, 대상 요약 · 사진 장수, 처리되면 상태 · 처리 시각, 상태로 거른다")
    void myReports() throws Exception {
        TestAccounts.Account provider = testAccounts.create(Role.PROVIDER);
        TestAccounts.Account admin = testAccounts.create(Role.ADMIN);
        TestAccounts.Account reporter = testAccounts.create(Role.USER);
        TestAccounts.Account other = testAccounts.create(Role.USER);
        long spaceId = spaceScenario.publishedSpace(provider, admin, Set.of());

        long spaceReportId = report(reporter, "SPACE", spaceId, TestFiles.jpeg("photos"))
                .andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        long memberReportId = report(reporter, "MEMBER", provider.id())
                .andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8).transform(this::idOf);
        report(other, "SPACE", spaceId).andExpect(status().isCreated());

        myReports(reporter, null)
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(memberReportId))
                .andExpect(jsonPath("$.data.content[0].targetSummary").value("통합테스트"))
                .andExpect(jsonPath("$.data.content[0].photoCount").value(0))
                .andExpect(jsonPath("$.data.content[1].id").value(spaceReportId))
                .andExpect(jsonPath("$.data.content[1].targetSummary").value("정왕역 앞 1층 상가"))
                .andExpect(jsonPath("$.data.content[1].photoCount").value(1))
                .andExpect(jsonPath("$.data.content[1].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[1].resolvedAt").isEmpty())
                .andExpect(jsonPath("$.data.content[1].adminNote").doesNotExist());

        mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", spaceReportId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\": \"DISMISS\", \"note\": \"확인 결과 문제 없음\"}"))
                .andExpect(status().isOk());

        myReports(reporter, "PENDING")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(memberReportId));
        myReports(reporter, "DISMISSED")
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("DISMISSED"))
                .andExpect(jsonPath("$.data.content[0].resolvedAt").isNotEmpty());
        myReports(other, null).andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("사진 6장이면 400 REPORT_PHOTO_LIMIT_EXCEEDED, 파일을 올리기 전에 막는다")
    void photoLimit() throws Exception {
        TestAccounts.Account writer = testAccounts.create(Role.USER);
        TestAccounts.Account reporter = testAccounts.create(Role.USER);
        Set<Path> before = storedReportPhotos();

        MockMultipartFile[] photos = new MockMultipartFile[6];
        Arrays.setAll(photos, i -> TestFiles.jpeg("photos"));
        report(reporter, "MEMBER", writer.id(), photos)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("REPORT_PHOTO_LIMIT_EXCEEDED"));

        assertThat(storedReportPhotos()).isEqualTo(before);
    }

    private ResultActions report(TestAccounts.Account reporter, String targetType, long targetId,
                                 MockMultipartFile... photos) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/reports");
        for (MockMultipartFile photo : photos) {
            request.file(photo);
        }
        return mockMvc.perform(request.part(text("targetType", targetType), text("targetId", String.valueOf(targetId)),
                        text("reason", "FALSE_INFORMATION"))
                .header(HttpHeaders.AUTHORIZATION, reporter.bearer()));
    }

    /** @param statusFilter null 이면 전체 */
    private ResultActions myReports(TestAccounts.Account account, String statusFilter) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/members/me/reports")
                .header(HttpHeaders.AUTHORIZATION, account.bearer());
        return mockMvc.perform(statusFilter == null ? request : request.param("status", statusFilter))
                .andExpect(status().isOk());
    }

    private static MockPart text(String name, String value) {
        return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private Set<Path> storedReportPhotos(long reportId) throws IOException {
        return storedReportPhotos().stream().filter(path -> path.toString().contains("/reports/" + reportId + "/"))
                .collect(Collectors.toSet());
    }

    /** 로컬 저장소(build/test-storage)의 신고 사진 전체. */
    private Set<Path> storedReportPhotos() throws IOException {
        Path reports = ((LocalFileStorage) fileStorage).root().resolve("private/reports");
        if (Files.notExists(reports)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(reports)) {
            return files.filter(Files::isRegularFile).collect(Collectors.toSet());
        }
    }

    private ResultActions login(TestAccounts.Account account) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(account.email(), TestAccounts.PASSWORD)));
    }

    private long idOf(String body) {
        try {
            return objectMapper.readTree(body).at("/data/id").asLong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
