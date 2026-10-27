package com.dasisiheung.controller;

import com.dasisiheung.dto.auth.LoginRequest;
import com.dasisiheung.dto.auth.RefreshTokenRequest;
import com.dasisiheung.dto.auth.SignupRequest;
import com.dasisiheung.dto.auth.TokenResponse;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.member.MemberSummaryResponse;
import com.dasisiheung.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 · 로그인 · 토큰")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이용자(USER) 또는 공간 제공자(PROVIDER)로 가입합니다. 관리자는 가입할 수 없습니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<MemberSummaryResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResult.ok(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "액세스 토큰(30분)과 리프레시 토큰(14일)을 발급합니다.")
    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @Operation(summary = "토큰 재발급",
            description = "리프레시 토큰으로 새 액세스·리프레시 토큰을 받습니다. 사용한 리프레시 토큰은 폐기되므로 새 값으로 바꿔 저장하세요.")
    @PostMapping("/refresh")
    public ApiResult<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResult.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기합니다. 액세스 토큰은 앱에서 삭제하세요.")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResult.ok();
    }
}
