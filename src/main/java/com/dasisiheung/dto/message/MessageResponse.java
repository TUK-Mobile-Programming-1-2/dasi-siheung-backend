package com.dasisiheung.dto.message;

import com.dasisiheung.entity.message.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MessageResponse(
        @Schema(description = "메시지 ID (클수록 최근)", example = "42") Long id,
        @Schema(description = "대화 ID", example = "1") Long conversationId,
        @Schema(description = "보낸 회원 ID", example = "1") Long senderId,
        @Schema(description = "보낸 회원 이름") String senderName,
        @Schema(description = "내가 보냈는지", example = "true") boolean mine,
        @Schema(description = "내용") String content,
        @Schema(description = "보낸 시각") LocalDateTime createdAt
) {

    public static MessageResponse of(Message message, Long viewerId) {
        return new MessageResponse(message.getId(), message.getConversation().getId(), message.getSender().getId(),
                message.getSender().getName(), message.getSender().getId().equals(viewerId), message.getContent(),
                message.getCreatedAt());
    }
}
