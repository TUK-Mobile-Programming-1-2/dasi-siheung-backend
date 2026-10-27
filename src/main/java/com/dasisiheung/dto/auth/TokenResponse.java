package com.dasisiheung.dto.auth;

import com.dasisiheung.dto.member.MemberSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

public record TokenResponse(
        @Schema(description = "토큰 종류. Authorization 헤더에 '{tokenType} {accessToken}' 로 보낸다", example = "Bearer")
        String tokenType,
        @Schema(description = "액세스 토큰 (JWT)") String accessToken,
        @Schema(description = "액세스 토큰 유효 시간(초)", example = "1800") long accessTokenExpiresIn,
        @Schema(description = "리프레시 토큰. 재발급할 때마다 새 값으로 바뀐다") String refreshToken,
        @Schema(description = "리프레시 토큰 유효 시간(초)", example = "1209600") long refreshTokenExpiresIn,
        @Schema(description = "로그인한 회원") MemberSummaryResponse member
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse of(String accessToken, Duration accessTokenValidity,
                                   String refreshToken, Duration refreshTokenValidity,
                                   MemberSummaryResponse member) {
        return new TokenResponse(BEARER, accessToken, accessTokenValidity.toSeconds(),
                refreshToken, refreshTokenValidity.toSeconds(), member);
    }
}
