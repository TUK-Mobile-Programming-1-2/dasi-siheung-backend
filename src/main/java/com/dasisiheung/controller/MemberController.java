package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.member.ChangePasswordRequest;
import com.dasisiheung.dto.member.MyProfileResponse;
import com.dasisiheung.dto.member.UpdateProfileRequest;
import com.dasisiheung.dto.member.WithdrawRequest;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Member", description = "내 정보")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회")
    @GetMapping
    public ApiResult<MyProfileResponse> getMyProfile(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(memberService.getMyProfile(loginMember.id()));
    }

    @Operation(summary = "내 정보 수정", description = "이름·휴대폰 번호 중 바꿀 값만 보냅니다.")
    @PatchMapping
    public ApiResult<MyProfileResponse> updateMyProfile(@LoginMember AuthMember loginMember,
                                                        @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResult.ok(memberService.updateMyProfile(loginMember.id(), request));
    }

    @Operation(summary = "프로필 사진 변경",
            description = "multipart 파트 image 에 사진 한 장. JPG·PNG·WEBP 5MB. 이전 사진은 지워집니다. "
                    + "글쓴이 · 대화 상대 정보에도 같은 주소가 내려갑니다.")
    @PostMapping(path = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<MyProfileResponse> changeProfileImage(@LoginMember AuthMember loginMember,
                                                           @RequestPart("image") MultipartFile image) {
        return ApiResult.ok(memberService.changeProfileImage(loginMember.id(), image));
    }

    @Operation(summary = "프로필 사진 삭제", description = "기본 이미지로 돌아갑니다. 사진이 없어도 200 입니다.")
    @DeleteMapping("/profile-image")
    public ApiResult<MyProfileResponse> deleteProfileImage(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(memberService.deleteProfileImage(loginMember.id()));
    }

    @Operation(summary = "비밀번호 변경", description = "변경하면 모든 기기의 리프레시 토큰이 폐기됩니다.")
    @PatchMapping("/password")
    public ApiResult<Void> changePassword(@LoginMember AuthMember loginMember,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        memberService.changePassword(loginMember.id(), request);
        return ApiResult.ok();
    }

    @Operation(summary = "회원 탈퇴", description = "개인정보(이메일·이름·연락처)는 즉시 지워지고 같은 계정으로 다시 로그인할 수 없습니다. "
            + "비밀번호가 틀리면 400 PASSWORD_MISMATCH, 진행 중인 신청 · 예약(검토 중이거나 이용이 끝나지 않은 확정 예약, "
            + "공간 제공자는 받은 신청 포함)이 있으면 409 WITHDRAWAL_BLOCKED_BY_APPLICATION.")
    @PostMapping("/withdraw")
    public ApiResult<Void> withdraw(@LoginMember AuthMember loginMember, @Valid @RequestBody WithdrawRequest request) {
        memberService.withdraw(loginMember.id(), request.password());
        return ApiResult.ok();
    }
}
