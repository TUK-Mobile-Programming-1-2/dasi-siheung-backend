package com.dasisiheung.dto.message;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.entity.message.Conversation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 대화 목록 항목 · 대화 정보. */
public record ConversationResponse(
        @Schema(description = "대화 ID", example = "1") Long id,
        @Schema(description = "공간 ID", example = "10") Long spaceId,
        @Schema(description = "공간 이름") String spaceTitle,
        @Schema(description = "공간 대표 사진 주소 (없으면 null)") String spaceThumbnailUrl,
        @Schema(description = "상대방") Counterpart counterpart,
        @Schema(description = "마지막 메시지 미리보기 (최대 100자, 없으면 null)") String lastMessagePreview,
        @Schema(description = "마지막 메시지 시각 (없으면 null)") LocalDateTime lastMessageAt,
        @Schema(description = "안 읽은 메시지 수", example = "2") long unreadCount,
        @Schema(description = "내가 마지막으로 읽은 메시지 ID (없으면 0)", example = "40") long myLastReadMessageId
) {

    /**
     * @param thumbnailUrl 공간 대표 사진 주소 (없으면 null)
     * @param counterpart  보는 사람의 상대방
     */
    public static ConversationResponse of(Conversation conversation, Long viewerId, String thumbnailUrl,
                                          Counterpart counterpart, long unreadCount) {
        return new ConversationResponse(conversation.getId(), conversation.getSpace().getId(),
                conversation.getSpace().getTitle(), thumbnailUrl, counterpart, conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(), unreadCount, conversation.lastReadMessageIdOf(viewerId));
    }

    public record Counterpart(
            @Schema(description = "회원 ID", example = "2") Long id,
            @Schema(description = "이름", example = "김제공") String name,
            @Schema(description = "역할", example = "PROVIDER") Role role,
            @Schema(description = "프로필 사진 주소 (없거나 탈퇴했으면 null)") String profileImageUrl
    ) {

        /** @param profileImageUrl 사진이 없으면 null */
        public static Counterpart of(Member member, String profileImageUrl) {
            return new Counterpart(member.getId(), member.getName(), member.getRole(), profileImageUrl);
        }
    }
}
