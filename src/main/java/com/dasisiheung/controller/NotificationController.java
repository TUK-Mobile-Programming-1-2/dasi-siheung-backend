package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.notification.NotificationCountResponse;
import com.dasisiheung.dto.notification.NotificationResponse;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 회원의 알림함. 가격 제안 · 수락 · 거절이 남고, 같은 내용이 푸시(data-only)로도 간다. */
@Tag(name = "Notification", description = "알림함")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록", description = "최근 순. unreadOnly=true 면 안 읽은 알림만. "
            + "항목을 누르면 applicationId 의 신청 상세를 열고 읽음 처리하세요.")
    @GetMapping
    public ApiResult<PageResponse<NotificationResponse>> getMyNotifications(
            @LoginMember AuthMember loginMember, @RequestParam(defaultValue = "false") boolean unreadOnly,
            @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(notificationService.getMyNotifications(loginMember.id(), unreadOnly, pageQuery));
    }

    @Operation(summary = "안 읽은 알림 수", description = "알림 아이콘 배지용.")
    @GetMapping("/unread-count")
    public ApiResult<NotificationCountResponse> getUnreadCount(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(notificationService.countUnread(loginMember.id()));
    }

    @Operation(summary = "알림 읽음 처리", description = "이미 읽은 알림이면 처음 읽은 시각을 그대로 돌려줍니다. "
            + "없거나 남의 알림이면 404 NOTIFICATION_NOT_FOUND.")
    @PostMapping("/{notificationId}/read")
    public ApiResult<NotificationResponse> markRead(@LoginMember AuthMember loginMember,
                                                    @PathVariable Long notificationId) {
        return ApiResult.ok(notificationService.markRead(loginMember.id(), notificationId));
    }

    @Operation(summary = "모두 읽음 처리", description = "이번에 읽음으로 바뀐 알림 수를 돌려줍니다.")
    @PostMapping("/read-all")
    public ApiResult<NotificationCountResponse> markAllRead(@LoginMember AuthMember loginMember) {
        return ApiResult.ok(notificationService.markAllRead(loginMember.id()));
    }
}
