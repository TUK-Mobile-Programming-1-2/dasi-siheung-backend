package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.message.ConversationResponse;
import com.dasisiheung.dto.message.MessageRequest;
import com.dasisiheung.dto.message.MessageResponse;
import com.dasisiheung.dto.message.ReadRequest;
import com.dasisiheung.dto.message.UnreadCountResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 1:1 소통 (폴링). 이용자가 공간별로 대화를 열고, 두 사람이 메시지를 주고받는다. */
@Tag(name = "Conversation", description = "1:1 소통 (폴링)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @Operation(summary = "대화 열기", description = "이용자가 공간 제공자에게 대화를 엽니다. 이미 있으면 그 대화를 돌려줍니다.")
    @PostMapping("/api/v1/spaces/{spaceId}/conversations")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<ConversationResponse> open(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(conversationService.open(loginMember.id(), spaceId));
    }

    @Operation(summary = "내 대화 목록", description = "최근 메시지 순. 각 대화의 안 읽은 수가 함께 내려갑니다.")
    @GetMapping("/api/v1/conversations")
    public ApiResult<PageResponse<ConversationResponse>> getMyConversations(
            @LoginMember AuthMember loginMember, @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(conversationService.getMyConversations(loginMember.id(), pageQuery));
    }

    @Operation(summary = "메시지 조회 (폴링)",
            description = "after: 그 ID 뒤 새 메시지 (5초마다), before: 그 ID 앞 지난 메시지, 둘 다 없으면 최근 메시지. 오래된 순.")
    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    public ApiResult<List<MessageResponse>> getMessages(
            @LoginMember AuthMember loginMember, @PathVariable Long conversationId,
            @RequestParam(required = false) Long after, @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResult.ok(conversationService.getMessages(loginMember.id(), conversationId, after, before, size));
    }

    @Operation(summary = "메시지 보내기")
    @PostMapping("/api/v1/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<MessageResponse> send(@LoginMember AuthMember loginMember, @PathVariable Long conversationId,
                                           @Valid @RequestBody MessageRequest request) {
        return ApiResult.ok(conversationService.send(loginMember.id(), conversationId, request.content()));
    }

    @Operation(summary = "읽음 처리", description = "화면에 보인 마지막 메시지 ID 를 보냅니다. 읽음 위치는 뒤로 돌아가지 않습니다.")
    @PostMapping("/api/v1/conversations/{conversationId}/read")
    public ApiResult<ConversationResponse> markRead(@LoginMember AuthMember loginMember,
                                                    @PathVariable Long conversationId,
                                                    @Valid @RequestBody ReadRequest request) {
        return ApiResult.ok(conversationService.markRead(loginMember.id(), conversationId, request.lastMessageId()));
    }

    @Operation(summary = "안 읽은 메시지 수", description = "앱 탭 배지용. 30초마다 조회하세요.")
    @GetMapping("/api/v1/conversations/unread-count")
    public ApiResult<UnreadCountResponse> getUnreadCount(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(conversationService.getUnreadCount(loginMember.id()));
    }
}
