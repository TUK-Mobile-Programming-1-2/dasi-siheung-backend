package com.dasisiheung.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.dasisiheung.support.FixedClockConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path root;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(new StorageProperties.Local(root, "http://localhost:8080/"),
                FixedClockConfig.fixedClock());
    }

    @Test
    @DisplayName("버킷별 폴더에 저장하고, 지우면 사라진다")
    void uploadAndDelete() throws Exception {
        storage.upload(StorageBucket.PRIVATE, "proofs/1/a.pdf", new byte[]{1, 2, 3}, "application/pdf");

        Path saved = root.resolve("private/proofs/1/a.pdf");
        assertThat(Files.readAllBytes(saved)).containsExactly(1, 2, 3);

        storage.delete(StorageBucket.PRIVATE, "proofs/1/a.pdf");
        assertThat(saved).doesNotExist();
    }

    @Test
    @DisplayName("주소는 /local-storage/{public|private}/{path} 이고 임시 주소에는 만료 시각이 붙는다")
    void urls() {
        assertThat(storage.publicUrl("spaces/1/a.jpg"))
                .isEqualTo("http://localhost:8080/local-storage/public/spaces/1/a.jpg");
        assertThat(storage.signedUrl("proofs/1/a.pdf", Duration.ofMinutes(10)))
                .isEqualTo("http://localhost:8080/local-storage/private/proofs/1/a.pdf?expires="
                        + FixedClockConfig.FIXED_NOW.plusSeconds(600).getEpochSecond());
    }

    @Test
    @DisplayName("저장소 폴더 밖을 가리키는 경로는 거부한다")
    void rejectsPathTraversal() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> storage.upload(StorageBucket.PUBLIC, "../../etc/passwd", new byte[]{1}, "text/plain"));
    }

    @Test
    @DisplayName("없는 파일을 지워도 오류가 아니다")
    void deleteMissingFile() {
        storage.delete(StorageBucket.PUBLIC, "nothing/here.jpg");
    }
}
