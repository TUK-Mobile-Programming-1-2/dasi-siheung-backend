package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.StorageBucket;
import com.dasisiheung.storage.StorageException;
import com.dasisiheung.storage.StorageProperties;
import com.dasisiheung.support.TestFiles;
import com.dasisiheung.util.FileType;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private FileStorage fileStorage;

    private FileUploadService service;

    @BeforeEach
    void setUp() {
        service = new FileUploadService(fileStorage,
                new StorageProperties(StorageProperties.Provider.LOCAL, Duration.ofMinutes(10), null, null));
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("형식을 내용으로 판별해 '폴더/UUID.확장자' 경로로 저장한다")
    void upload() {
        StoredFile stored = service.upload(UploadPolicy.SPACE_PHOTO, "spaces/1/photos", TestFiles.jpeg("file"));

        assertThat(stored.path()).matches("spaces/1/photos/[0-9a-f-]{36}\\.jpg");
        assertThat(stored.type()).isEqualTo(FileType.JPEG);
        assertThat(stored.originalName()).isEqualTo("photo.jpg");
        then(fileStorage).should().upload(eq(StorageBucket.PUBLIC), eq(stored.path()), any(), eq("image/jpeg"));
    }

    @Test
    @DisplayName("확장자만 이미지인 파일은 UNSUPPORTED_FILE_TYPE")
    void rejectsFakeImage() {
        assertThatThrownBy(() -> service.upload(UploadPolicy.SPACE_PHOTO, "spaces/1/photos", TestFiles.fakeImage("file")))
                .extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
        then(fileStorage).should(never()).upload(any(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("사진 자리에 PDF 는 올릴 수 없고, 증빙에는 올릴 수 있다")
    void pdfOnlyForProofs() {
        assertThatThrownBy(() -> service.upload(UploadPolicy.SPACE_PHOTO, "d", TestFiles.pdf("file")))
                .extracting("errorCode").isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);

        StoredFile proof = service.upload(UploadPolicy.SPACE_PROOF, "proofs/1", TestFiles.pdf("file"));
        assertThat(proof.path()).endsWith(".pdf");
    }

    @Test
    @DisplayName("빈 파일은 FILE_REQUIRED, 정책보다 크면 FILE_TOO_LARGE")
    void sizeChecks() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        MockMultipartFile huge = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[0]) {
            @Override
            public long getSize() {
                return UploadPolicy.SPACE_PHOTO.getMaxBytes() + 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };

        assertThatThrownBy(() -> service.upload(UploadPolicy.SPACE_PHOTO, "d", empty))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_REQUIRED);
        assertThatThrownBy(() -> service.upload(UploadPolicy.SPACE_PHOTO, "d", huge))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("저장소 장애는 STORAGE_UNAVAILABLE")
    void storageDown() {
        willThrow(new StorageException("down")).given(fileStorage).upload(any(), anyString(), any(), anyString());

        assertThatThrownBy(() -> service.upload(UploadPolicy.SPACE_PHOTO, "d", TestFiles.jpeg("file")))
                .extracting("errorCode").isEqualTo(ErrorCode.STORAGE_UNAVAILABLE);
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 방금 올린 파일을 지운다")
    void deletesUploadedFileOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        StoredFile stored = service.upload(UploadPolicy.SPACE_PROOF, "proofs/1", TestFiles.pdf("file"));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        then(fileStorage).should().delete(StorageBucket.PRIVATE, stored.path());
    }

    @Test
    @DisplayName("삭제는 커밋된 뒤에만 실행되고, 롤백되면 지우지 않는다")
    void deleteAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        service.deleteAfterCommit(UploadPolicy.SPACE_PHOTO, "spaces/1/a.jpg");
        then(fileStorage).should(never()).delete(any(), anyString());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        then(fileStorage).should().delete(StorageBucket.PUBLIC, "spaces/1/a.jpg");
    }

    @Test
    @DisplayName("공개 파일은 고정 주소, 비공개 파일은 임시 주소")
    void urls() {
        given(fileStorage.publicUrl("spaces/1/a.jpg")).willReturn("https://cdn/a.jpg");
        given(fileStorage.signedUrl("proofs/1/a.pdf", Duration.ofMinutes(10))).willReturn("https://cdn/a.pdf?token=t");

        assertThat(service.url(UploadPolicy.SPACE_PHOTO, "spaces/1/a.jpg")).isEqualTo("https://cdn/a.jpg");
        assertThat(service.url(UploadPolicy.SPACE_PROOF, "proofs/1/a.pdf")).isEqualTo("https://cdn/a.pdf?token=t");
    }
}
