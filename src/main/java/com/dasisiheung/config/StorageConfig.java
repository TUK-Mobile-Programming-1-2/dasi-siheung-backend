package com.dasisiheung.config;

import com.dasisiheung.storage.FileStorage;
import com.dasisiheung.storage.LocalFileStorage;
import com.dasisiheung.storage.StorageProperties;
import com.dasisiheung.storage.SupabaseFileStorage;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** app.storage.provider 에 따라 저장소 구현을 고른다. (local: 로컬 디스크, supabase: Supabase Storage) */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public FileStorage fileStorage(StorageProperties properties, RestClient.Builder restClientBuilder, Clock clock) {
        return switch (properties.provider()) {
            case SUPABASE -> new SupabaseFileStorage(properties.supabase(),
                    restClientBuilder.requestFactory(requestFactory()));
            case LOCAL -> new LocalFileStorage(properties.local(), clock);
        };
    }

    /**
     * 로컬 저장소를 쓸 때만 /local-storage/** 로 저장된 파일을 내려준다. (운영의 Supabase 공개 주소 역할)
     * WebMvcConfigurer 를 클래스로 만들면 @WebMvcTest 슬라이스에 섞여 들어가므로 빈 메서드로 등록한다.
     */
    @Bean
    public WebMvcConfigurer localStorageResourceHandler(FileStorage fileStorage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                if (fileStorage instanceof LocalFileStorage localFileStorage) {
                    registry.addResourceHandler(LocalFileStorage.URL_PREFIX + "**")
                            .addResourceLocations(directoryLocation(localFileStorage));
                }
            }
        };
    }

    private static String directoryLocation(LocalFileStorage storage) {
        String location = storage.root().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
