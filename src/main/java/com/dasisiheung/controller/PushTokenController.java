package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.push.PushTokenRequest;
import com.dasisiheung.entity.member.PushToken;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.PushTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PushToken", description = "푸시 알림 기기 토큰")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/members/me/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @Operation(summary = "기기 토큰 등록",
            description = "앱 시작 · 로그인 직후 · onNewToken 때 호출합니다. 같은 토큰을 여러 번 보내도 되고, "
                    + "다른 계정이 쓰던 토큰이면 지금 로그인한 계정으로 옮겨집니다.")
    @PutMapping
    public ApiResult<Void> register(@LoginMember AuthMember loginMember,
                                    @Valid @RequestBody PushTokenRequest request) {
        pushTokenService.register(loginMember.id(), request.token());
        return ApiResult.ok();
    }

    @Operation(summary = "기기 토큰 해제", description = "로그아웃 API 를 부르기 전에 호출합니다. 이미 없어도 200 입니다.")
    @DeleteMapping
    public ApiResult<Void> unregister(@LoginMember AuthMember loginMember,
                                      @RequestParam @NotBlank @Size(max = PushToken.MAX_TOKEN_LENGTH) String token) {
        pushTokenService.unregister(loginMember.id(), token);
        return ApiResult.ok();
    }
}
