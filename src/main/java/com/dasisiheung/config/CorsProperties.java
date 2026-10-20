package com.dasisiheung.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 웹(React)처럼 브라우저에서 호출하는 클라이언트의 허용 출처 목록.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
