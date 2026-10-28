package com.dasisiheung.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 개발·CI 용 저장소. 파일을 디스크에 저장하고 /local-storage/** 로 내려준다.
 * 임시 주소의 만료 시각은 표시만 하고 검사하지 않는다. (운영에서는 쓰지 않음)
 */
@Slf4j
public class LocalFileStorage implements FileStorage {

    public static final String URL_PREFIX = "/local-storage/";

    private final Path root;
    private final String baseUrl;
    private final Clock clock;

    public LocalFileStorage(StorageProperties.Local properties, Clock clock) {
        this.root = properties.root().toAbsolutePath().normalize();
        this.baseUrl = properties.baseUrl();
        this.clock = clock;
    }

    @Override
    public void upload(StorageBucket bucket, String path, byte[] content, String contentType) {
        Path target = resolve(bucket, path);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new StorageException("로컬 저장소에 파일을 쓰지 못했습니다: " + path, e);
        }
    }

    @Override
    public String publicUrl(String path) {
        return url(StorageBucket.PUBLIC, path);
    }

    @Override
    public String signedUrl(String path, Duration ttl) {
        long expiresAt = clock.instant().plus(ttl).getEpochSecond();
        return url(StorageBucket.PRIVATE, path) + "?expires=" + expiresAt;
    }

    @Override
    public void delete(StorageBucket bucket, String path) {
        try {
            Files.deleteIfExists(resolve(bucket, path));
        } catch (IOException e) {
            throw new StorageException("로컬 저장소의 파일을 지우지 못했습니다: " + path, e);
        }
    }

    @Override
    public void initialize() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            log.warn("로컬 저장소 폴더를 만들지 못했습니다: {}", root, e);
        }
        log.info("파일 저장소: 로컬 디스크 ({})", root);
    }

    public Path root() {
        return root;
    }

    private Path resolve(StorageBucket bucket, String path) {
        Path target = root.resolve(directoryOf(bucket)).resolve(path).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("저장소 밖의 경로입니다: " + path);
        }
        return target;
    }

    private String url(StorageBucket bucket, String path) {
        return baseUrl + URL_PREFIX + directoryOf(bucket) + "/" + path;
    }

    private static String directoryOf(StorageBucket bucket) {
        return bucket.name().toLowerCase(Locale.ROOT);
    }
}
