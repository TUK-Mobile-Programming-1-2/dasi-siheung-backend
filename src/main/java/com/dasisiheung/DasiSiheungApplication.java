package com.dasisiheung;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 인증은 JWT 기반으로 직접 구성하므로 기본 인메모리 사용자 생성을 끈다.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DasiSiheungApplication {

    public static void main(String[] args) {
        SpringApplication.run(DasiSiheungApplication.class, args);
    }
}
