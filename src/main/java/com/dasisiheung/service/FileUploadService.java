package com.dasisiheung.service;

import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.StorageBucket;
import com.dasisiheung.storage.StorageException;
import com.dasisiheung.storage.StorageProperties;
import com.dasisiheung.util.FileType;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 검증(형식·크기)과 저장소 호출을 맡는다. 다른 서비스는 이 클래스만 통해 파일을 다룬다.
 * <p>
 * DB 저장과 파일 저장을 맞추기 위해, 트랜잭션 안에서 호출하면
 * <ul>
 *   <li>업로드 후 트랜잭션이 롤백되면 올린 파일을 지우고</li>
 *   <li>삭제는 트랜잭션이 커밋된 뒤에 실제로 지운다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileStorage fileStorage;
    private final StorageProperties storageProperties;

    /**
     * @param directory 버킷 안의 폴더 (예: spaces/1/photos)
     */
    public StoredFile upload(UploadPolicy policy, String directory, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_REQUIRED);
        }
        if (file.getSize() > policy.getMaxBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "파일은 %dMB 이하만 올릴 수 있습니다.".formatted(policy.maxMegabytes()));
        }
        byte[] content = readBytes(file);
        FileType type = FileType.detect(content)
                .filter(policy::allows)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                        "올릴 수 있는 형식: " + policy.getAllowedTypes()));

        String path = directory + "/" + UUID.randomUUID() + "." + type.extension();
        try {
            fileStorage.upload(policy.getBucket(), path, content, type.contentType());
        } catch (StorageException e) {
            log.error("파일 업로드 실패", e);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
        }
        deleteIfRolledBack(policy.getBucket(), path);
        return new StoredFile(path, type, file.getOriginalFilename(), content.length);
    }

    /** 공개 파일은 고정 주소, 비공개 파일은 임시 주소(기본 10분)를 돌려준다. */
    public String url(UploadPolicy policy, String path) {
        if (policy.getBucket() == StorageBucket.PUBLIC) {
            return fileStorage.publicUrl(path);
        }
        try {
            return fileStorage.signedUrl(path, storageProperties.signedUrlTtl());
        } catch (StorageException e) {
            log.error("임시 주소 발급 실패", e);
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    /** 트랜잭션이 커밋된 뒤 지운다. 실패해도 요청은 성공으로 두고 로그만 남긴다. (고아 파일은 운영자가 정리) */
    public void deleteAfterCommit(UploadPolicy policy, String path) {
        runAfterTransaction(TransactionSynchronization.STATUS_COMMITTED,
                () -> deleteQuietly(policy.getBucket(), path));
    }

    private void deleteIfRolledBack(StorageBucket bucket, String path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(bucket, path);
                    }
                }
            });
        }
    }

    private void runAfterTransaction(int expectedStatus, Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == expectedStatus) {
                    action.run();
                }
            }
        });
    }

    private void deleteQuietly(StorageBucket bucket, String path) {
        try {
            fileStorage.delete(bucket, path);
        } catch (StorageException e) {
            log.warn("파일 삭제 실패 (수동 정리 필요): {}/{}", bucket, path, e);
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일을 읽지 못했습니다.");
        }
    }
}
