package com.dasisiheung.dto.push;

import com.dasisiheung.entity.member.PushToken;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(
        @Schema(description = "FCM 등록 토큰 (FirebaseMessaging.getInstance().token)", example = "fcm-token-from-device")
        @NotBlank @Size(max = PushToken.MAX_TOKEN_LENGTH) String token
) {
}
