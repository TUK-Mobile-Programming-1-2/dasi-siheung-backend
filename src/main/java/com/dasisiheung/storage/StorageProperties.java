package com.dasisiheung.storage;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 파일 저장소 설정.
 *
 * @param provider     local(로컬 디스크, 기본) | supabase(Supabase Storage)
 * @param signedUrlTtl 비공개 파일 임시 주소 유효 시간 (기본 10분)
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(Provider provider, Duration signedUrlTtl, Supabase supabase, Local local) {

    private static final Duration DEFAULT_SIGNED_URL_TTL = Duration.ofMinutes(10);

    public StorageProperties {
        provider = provider == null ? Provider.LOCAL : provider;
        signedUrlTtl = signedUrlTtl == null ? DEFAULT_SIGNED_URL_TTL : signedUrlTtl;
        supabase = supabase == null ? new Supabase(null, null, null, null) : supabase;
        local = local == null ? new Local(null, null) : local;
    }

    public enum Provider {
        LOCAL,
        SUPABASE
    }

    /**
     * @param url           https://{project-ref}.supabase.co
     * @param serviceKey    service_role 키 또는 secret 키 (sb_secret_...). 서버에만 둔다.
     * @param publicBucket  공개 버킷 이름 (기본 space-photos)
     * @param privateBucket 비공개 버킷 이름 (기본 private-files)
     */
    public record Supabase(String url, String serviceKey, String publicBucket, String privateBucket) {

        public Supabase {
            url = url == null ? null : url.replaceAll("/+$", "");
            publicBucket = publicBucket == null || publicBucket.isBlank() ? "space-photos" : publicBucket;
            privateBucket = privateBucket == null || privateBucket.isBlank() ? "private-files" : privateBucket;
        }

        public String bucketName(StorageBucket bucket) {
            return bucket == StorageBucket.PUBLIC ? publicBucket : privateBucket;
        }
    }

    /**
     * @param root    저장 폴더 (기본 build/local-storage)
     * @param baseUrl 파일 주소 앞부분 (기본 http://localhost:8080)
     */
    public record Local(Path root, String baseUrl) {

        public Local {
            root = root == null ? Path.of("build", "local-storage") : root;
            baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : baseUrl.replaceAll("/+$", "");
        }
    }
}
