package com.dasisiheung.controller;

import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.option.OptionsResponse;
import com.dasisiheung.service.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Option", description = "선택지 (코드 · 이름)")
@RestController
@RequestMapping("/api/v1/options")
@RequiredArgsConstructor
public class OptionController {

    private static final Duration CACHE_DURATION = Duration.ofHours(1);

    private final OptionService optionService;

    @Operation(summary = "선택지 조회", description = "지역·허용 활동·시설·용도·상권 테마·이용 목적 등 화면 선택 목록. 앱 시작 시 한 번 받아 두세요.")
    @GetMapping
    public ResponseEntity<ApiResult<OptionsResponse>> getOptions() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_DURATION).cachePublic())
                .body(ApiResult.ok(optionService.getOptions()));
    }
}
