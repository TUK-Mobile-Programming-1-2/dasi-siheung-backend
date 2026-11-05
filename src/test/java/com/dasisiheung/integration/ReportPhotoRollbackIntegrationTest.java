package com.dasisiheung.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.entity.member.Role;
import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.LocalFileStorage;
import com.dasisiheung.support.IntegrationTest;
import com.dasisiheung.support.TestAccounts;
import com.dasisiheung.support.TestFiles;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 신고 사진 업로드 중 실패하면 신고도 롤백되고, 먼저 올린 파일도 지워진다.
 * 실제 커밋 · 롤백을 확인해야 하므로 테스트 트랜잭션을 쓰지 않는다. (만든 회원 · 신고는 테스트 DB 에 남는다)
 */
@IntegrationTest
class ReportPhotoRollbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts testAccounts;

    @Autowired
    private FileStorage fileStorage;

    @Test
    @DisplayName("두 번째 사진이 이미지가 아니면 400 → 신고는 저장되지 않고 첫 사진 파일도 지워져 다시 신고할 수 있다")
    void rollsBackReportAndFiles() throws Exception {
        TestAccounts.Account target = testAccounts.create(Role.USER);
        TestAccounts.Account reporter = testAccounts.create(Role.USER);
        Set<Path> before = storedReportPhotos();

        report(reporter, target.id(), TestFiles.jpeg("photos"), TestFiles.fakeImage("photos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
        assertThat(storedReportPhotos()).isEqualTo(before);

        report(reporter, target.id(), TestFiles.jpeg("photos"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photos.length()").value(1));
    }

    private ResultActions report(TestAccounts.Account reporter, long memberId, MockMultipartFile... photos)
            throws Exception {
        var request = multipart("/api/v1/reports");
        for (MockMultipartFile photo : photos) {
            request.file(photo);
        }
        return mockMvc.perform(request.part(text("targetType", "MEMBER"), text("targetId", String.valueOf(memberId)),
                        text("reason", "ABUSIVE"))
                .header(HttpHeaders.AUTHORIZATION, reporter.bearer()));
    }

    private static MockPart text(String name, String value) {
        return new MockPart(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private Set<Path> storedReportPhotos() throws IOException {
        Path reports = ((LocalFileStorage) fileStorage).root().resolve("private/reports");
        if (Files.notExists(reports)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(reports)) {
            return files.filter(Files::isRegularFile).collect(Collectors.toSet());
        }
    }
}
