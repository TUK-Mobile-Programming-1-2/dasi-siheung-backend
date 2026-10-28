package com.dasisiheung.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseFileStorageTest {

    private static final String URL = "https://abc.supabase.co";
    private static final String SECRET_KEY = "sb_secret_test";
    private static final String LEGACY_KEY = "eyJhbGciOiJIUzI1NiJ9.e30.signature";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    @DisplayName("업로드: 버킷/경로로 POST 하고, 새 형식 키는 apikey 헤더로만 보낸다")
    void uploadWithSecretKey() {
        SupabaseFileStorage storage = storage(SECRET_KEY);
        server.expect(requestTo(URL + "/storage/v1/object/space-photos/spaces/1/a.jpg"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", SECRET_KEY))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(header("x-upsert", "false"))
                .andRespond(withSuccess("{\"Key\":\"space-photos/spaces/1/a.jpg\"}", MediaType.APPLICATION_JSON));

        storage.upload(StorageBucket.PUBLIC, "spaces/1/a.jpg", new byte[]{1}, "image/jpeg");

        server.verify();
    }

    @Test
    @DisplayName("예전 형식(service_role JWT) 키는 Authorization: Bearer 도 함께 보낸다")
    void uploadWithLegacyKey() {
        SupabaseFileStorage storage = storage(LEGACY_KEY);
        server.expect(requestTo(URL + "/storage/v1/object/private-files/proofs/1/a.pdf"))
                .andExpect(header("apikey", LEGACY_KEY))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + LEGACY_KEY))
                .andRespond(withSuccess());

        storage.upload(StorageBucket.PRIVATE, "proofs/1/a.pdf", new byte[]{1}, "application/pdf");

        server.verify();
    }

    @Test
    @DisplayName("임시 주소: sign API 가 돌려준 상대 경로를 전체 주소로 바꾼다")
    void signedUrl() {
        SupabaseFileStorage storage = storage(SECRET_KEY);
        server.expect(requestTo(URL + "/storage/v1/object/sign/private-files/proofs/1/a.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"expiresIn\": 600}"))
                .andRespond(withSuccess("{\"signedURL\":\"/object/sign/private-files/proofs/1/a.pdf?token=t\"}",
                        MediaType.APPLICATION_JSON));

        String url = storage.signedUrl("proofs/1/a.pdf", Duration.ofMinutes(10));

        assertThat(url).isEqualTo(URL + "/storage/v1/object/sign/private-files/proofs/1/a.pdf?token=t");
    }

    @Test
    @DisplayName("공개 주소는 API 호출 없이 만든다")
    void publicUrl() {
        assertThat(storage(SECRET_KEY).publicUrl("spaces/1/a.jpg"))
                .isEqualTo(URL + "/storage/v1/object/public/space-photos/spaces/1/a.jpg");
    }

    @Test
    @DisplayName("삭제: 버킷에 prefixes 본문으로 DELETE")
    void delete() {
        SupabaseFileStorage storage = storage(SECRET_KEY);
        server.expect(requestTo(URL + "/storage/v1/object/space-photos"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("{\"prefixes\": [\"spaces/1/a.jpg\"]}"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        storage.delete(StorageBucket.PUBLIC, "spaces/1/a.jpg");

        server.verify();
    }

    @Test
    @DisplayName("저장소 오류는 StorageException 으로 감싼다")
    void wrapsErrors() {
        SupabaseFileStorage storage = storage(SECRET_KEY);
        server.expect(requestTo(URL + "/storage/v1/object/space-photos/a.jpg")).andRespond(withServerError());

        assertThatThrownBy(() -> storage.upload(StorageBucket.PUBLIC, "a.jpg", new byte[]{1}, "image/jpeg"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("기동 시 없는 버킷은 만들고 있는 버킷은 그대로 둔다")
    void initializeCreatesMissingBucket() {
        SupabaseFileStorage storage = storage(SECRET_KEY);
        server.expect(requestTo(URL + "/storage/v1/bucket/space-photos")).andRespond(withSuccess());
        server.expect(requestTo(URL + "/storage/v1/bucket/private-files"))
                .andRespond(withBadRequest().body("{\"statusCode\":\"404\",\"error\":\"Bucket not found\"}"));
        server.expect(requestTo(URL + "/storage/v1/bucket"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"id\":\"private-files\",\"public\":false}"))
                .andRespond(withSuccess());

        storage.initialize();

        server.verify();
    }

    @Test
    @DisplayName("URL 이나 키가 없으면 기동 단계에서 거부한다")
    void requiresConfiguration() {
        assertThatIllegalStateException().isThrownBy(() ->
                new SupabaseFileStorage(new StorageProperties.Supabase(URL, " ", null, null), RestClient.builder()));
    }

    private SupabaseFileStorage storage(String key) {
        return new SupabaseFileStorage(new StorageProperties.Supabase(URL + "/", key, null, null), builder);
    }
}
