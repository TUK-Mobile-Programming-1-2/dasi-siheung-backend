package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.member.BlockedMemberResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.MemberBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 차단. 차단하면 두 사람은 서로 메시지를 보내거나 새 대화를 열 수 없다. (이미 있는 대화방 · 메시지는 그대로 보인다)
 */
@Tag(name = "Block", description = "사용자 차단")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/members/me/blocks")
@RequiredArgsConstructor
public class MemberBlockController {

    private final MemberBlockService memberBlockService;

    @Operation(summary = "차단하기",
            description = "이미 차단했으면 그대로 둡니다(멱등). 차단하면 서로 메시지를 보내거나 새 대화를 열 수 없고 "
                    + "(403 BLOCKED_MEMBER) 상대의 메시지 푸시도 오지 않습니다. 이미 있는 대화방 · 메시지는 그대로 보입니다.")
    @PutMapping("/{memberId}")
    public ApiResult<BlockedMemberResponse> block(@LoginMember AuthMember loginMember, @PathVariable Long memberId) {
        return ApiResult.ok(memberBlockService.block(loginMember.id(), memberId));
    }

    @Operation(summary = "차단 해제", description = "차단하지 않은 상대여도 성공합니다.")
    @DeleteMapping("/{memberId}")
    public ApiResult<Void> unblock(@LoginMember AuthMember loginMember, @PathVariable Long memberId) {
        memberBlockService.unblock(loginMember.id(), memberId);
        return ApiResult.ok();
    }

    @Operation(summary = "차단한 사용자 목록", description = "최근 차단 순. 탈퇴한 회원도 남아 해제할 수 있습니다.")
    @GetMapping
    public ApiResult<PageResponse<BlockedMemberResponse>> getMyBlocks(@LoginMember AuthMember loginMember,
                                                                      @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(memberBlockService.getMyBlocks(loginMember.id(), pageQuery));
    }
}
