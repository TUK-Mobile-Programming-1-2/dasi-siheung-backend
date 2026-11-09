package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.member.NotificationSettingRequest;
import com.dasisiheung.dto.member.NotificationSettingResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "NotificationSetting", description = "알림 설정")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/members/me/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @Operation(summary = "내 알림 설정 조회", description = "저장한 적이 없으면 기본값을 주고 updatedAt 은 null 입니다.")
    @GetMapping
    public ApiResult<NotificationSettingResponse> getMySettings(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(notificationSettingService.getMySettings(loginMember.id()));
    }

    @Operation(summary = "내 알림 설정 변경",
            description = "모든 항목을 보내야 합니다(전체 교체). 시각은 0~23, 시작 > 끝이면 자정을 넘는 구간입니다. "
                    + "방해금지를 켜면 시작과 끝이 달라야 합니다. 범위를 벗어나거나 빠진 항목이 있으면 400 INVALID_INPUT.")
    @PutMapping
    public ApiResult<NotificationSettingResponse> replaceMySettings(@LoginMember AuthMember loginMember,
                                                                    @Valid @RequestBody NotificationSettingRequest request) {
        return ApiResult.ok(notificationSettingService.replaceMySettings(loginMember.id(), request));
    }
}
