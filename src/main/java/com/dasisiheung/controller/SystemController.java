package com.dasisiheung.controller;

import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.system.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System", description = "서버 상태 확인")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final Clock clock;

    @Operation(
            summary = "서버 연결 확인",
            description = "앱·관리자 웹이 백엔드와 통신할 수 있는지 확인합니다. 인증 없이 호출할 수 있습니다."
    )
    @GetMapping("/ping")
    public ApiResult<PingResponse> ping() {
        return ApiResult.ok(PingResponse.at(OffsetDateTime.now(clock)));
    }
}
