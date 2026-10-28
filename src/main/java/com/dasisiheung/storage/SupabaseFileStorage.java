package com.dasisiheung.storage;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Supabase Storage REST API 연동.
 * <pre>
 * 업로드   POST   /storage/v1/object/{bucket}/{path}
 * 임시 주소 POST   /storage/v1/object/sign/{bucket}/{path}   { "expiresIn": 초 }
 * 삭제     DELETE /storage/v1/object/{bucket}               { "prefixes": [path] }
 * 공개 주소        /storage/v1/object/public/{bucket}/{path}
 * </pre>
 * 키는 apikey 헤더로 보낸다. 예전 형식(service_role JWT)이면 Authorization: Bearer 도 함께 보낸다.
 * 새 형식(sb_secret_...)은 JWT 가 아니므로 Bearer 로 보내면 거부된다.
 */
@Slf4j
public class SupabaseFileStorage implements FileStorage {

    private static final String API_KEY_HEADER = "apikey";
    private static final String UPSERT_HEADER = "x-upsert";
    private static final String LEGACY_JWT_PREFIX = "eyJ";
    private static final long PUBLIC_FILE_SIZE_LIMIT = 10L * 1024 * 1024;
    private static final long PRIVATE_FILE_SIZE_LIMIT = 20L * 1024 * 1024;

    private final StorageProperties.Supabase properties;
    private final String storageUrl;
    private final RestClient restClient;

    public SupabaseFileStorage(StorageProperties.Supabase properties, RestClient.Builder restClientBuilder) {
        if (isBlank(properties.url()) || isBlank(properties.serviceKey())) {
            throw new IllegalStateException(
                    "app.storage.provider=supabase 이면 SUPABASE_URL, SUPABASE_SERVICE_KEY 환경 변수가 필요합니다.");
        }
        this.properties = properties;
        this.storageUrl = properties.url() + "/storage/v1";
        String serviceKey = properties.serviceKey();
        this.restClient = restClientBuilder
                .defaultHeader(API_KEY_HEADER, serviceKey)
                .defaultHeaders(headers -> {
                    if (serviceKey.startsWith(LEGACY_JWT_PREFIX)) {
                        headers.setBearerAuth(serviceKey);
                    }
                })
                .build();
    }

    @Override
    public void upload(StorageBucket bucket, String path, byte[] content, String contentType) {
        try {
            restClient.post()
                    .uri(objectUri("object", properties.bucketName(bucket), path))
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(UPSERT_HEADER, "false")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StorageException("Supabase Storage 업로드 실패: " + path, e);
        }
    }

    @Override
    public String publicUrl(String path) {
        return objectUri("object/public", properties.publicBucket(), path).toString();
    }

    @Override
    public String signedUrl(String path, Duration ttl) {
        try {
            SignedUrlResponse response = restClient.post()
                    .uri(objectUri("object/sign", properties.privateBucket(), path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", ttl.toSeconds()))
                    .retrieve()
                    .body(SignedUrlResponse.class);
            if (response == null || isBlank(response.signedURL())) {
                throw new StorageException("Supabase Storage 가 임시 주소를 돌려주지 않았습니다: " + path);
            }
            return storageUrl + response.signedURL();
        } catch (RestClientException e) {
            throw new StorageException("Supabase Storage 임시 주소 발급 실패: " + path, e);
        }
    }

    @Override
    public void delete(StorageBucket bucket, String path) {
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(objectUri("object", properties.bucketName(bucket), null))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(path)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StorageException("Supabase Storage 삭제 실패: " + path, e);
        }
    }

    /** 버킷이 없으면 만든다. 대시보드에서 직접 만들어 두었다면 아무것도 하지 않는다. */
    @Override
    public void initialize() {
        ensureBucket(properties.publicBucket(), true, PUBLIC_FILE_SIZE_LIMIT);
        ensureBucket(properties.privateBucket(), false, PRIVATE_FILE_SIZE_LIMIT);
    }

    private void ensureBucket(String bucketName, boolean isPublic, long fileSizeLimit) {
        try {
            if (bucketExists(bucketName)) {
                log.info("파일 저장소: Supabase 버킷 '{}' 확인", bucketName);
                return;
            }
            restClient.post()
                    .uri(storageUrl + "/bucket")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("id", bucketName, "name", bucketName, "public", isPublic,
                            "file_size_limit", fileSizeLimit))
                    .retrieve()
                    .toBodilessEntity();
            log.info("파일 저장소: Supabase 버킷 '{}' 생성 (공개: {})", bucketName, isPublic);
        } catch (RestClientException e) {
            log.warn("파일 저장소: Supabase 버킷 '{}' 을 확인·생성하지 못했습니다. 대시보드에서 직접 만들어 주세요. ({})",
                    bucketName, e.getMessage());
        }
    }

    private boolean bucketExists(String bucketName) {
        try {
            restClient.get().uri(storageUrl + "/bucket/" + bucketName).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            HttpStatusCode status = e.getStatusCode();
            // 없는 버킷은 400(본문 statusCode 404) 또는 404 로 응답한다
            if (status.value() == 400 || status.value() == 404) {
                return false;
            }
            throw e;
        }
    }

    private URI objectUri(String operation, String bucketName, String path) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(storageUrl)
                .path("/" + operation)
                .pathSegment(bucketName);
        if (path != null) {
            builder.pathSegment(path.split("/"));
        }
        return builder.encode().build().toUri();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SignedUrlResponse(String signedURL) {
    }
}
