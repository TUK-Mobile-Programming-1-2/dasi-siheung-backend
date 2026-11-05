package com.dasisiheung.service;

import com.dasisiheung.storage.StorageBucket;
import com.dasisiheung.util.FileType;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 업로드 종류별 저장 위치 · 허용 형식 · 최대 크기. */
@Getter
@RequiredArgsConstructor
public enum UploadPolicy {

    SPACE_PHOTO(StorageBucket.PUBLIC, 10L * 1024 * 1024, Set.of(FileType.JPEG, FileType.PNG, FileType.WEBP)),
    SPACE_PROOF(StorageBucket.PRIVATE, 20L * 1024 * 1024, Set.of(FileType.PDF, FileType.JPEG, FileType.PNG)),
    ACTIVITY_PHOTO(StorageBucket.PRIVATE, 10L * 1024 * 1024, Set.of(FileType.JPEG, FileType.PNG, FileType.WEBP)),
    REPORT_PHOTO(StorageBucket.PRIVATE, 10L * 1024 * 1024, Set.of(FileType.JPEG, FileType.PNG, FileType.WEBP));

    private final StorageBucket bucket;
    private final long maxBytes;
    private final Set<FileType> allowedTypes;

    public boolean allows(FileType type) {
        return allowedTypes.contains(type);
    }

    public long maxMegabytes() {
        return maxBytes / (1024 * 1024);
    }
}
