package com.dasisiheung.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record PingResponse(
        @Schema(description = "서버 상태", example = "OK") String status,
        @Schema(description = "서버 시각 (ISO-8601, Asia/Seoul)", example = "2026-10-20T21:00:00+09:00") OffsetDateTime serverTime
) {

    private static final String STATUS_OK = "OK";

    public static PingResponse at(OffsetDateTime serverTime) {
        return new PingResponse(STATUS_OK, serverTime);
    }
}
