package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.message.Conversation;
import com.dasisiheung.entity.message.Message;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConversationTest {

    private final Member user = ApplicationFixtures.applicant(1L);
    private final Conversation conversation = new Conversation(
            SpaceFixtures.space(10L, SpaceFixtures.provider(2L), Set.of(), FixedClockConfig.FIXED_TODAY), user);

    @Test
    @DisplayName("상대방 · 참여자 판단")
    void participants() {
        assertThat(conversation.counterpartOf(1L).getId()).isEqualTo(2L);
        assertThat(conversation.counterpartOf(2L).getId()).isEqualTo(1L);
        assertThatThrownBy(() -> conversation.validateParticipant(99L))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_CONVERSATION_MEMBER);
    }

    @Test
    @DisplayName("보내면 미리보기(100자) · 시각이 바뀌고 보낸 사람은 그 메시지까지 읽음")
    void onMessageSent() {
        Message message = new Message(conversation, user, "가".repeat(150), FixedClockConfig.FIXED_LOCAL_NOW);
        ReflectionTestUtils.setField(message, "id", 7L);

        conversation.onMessageSent(message);

        assertThat(conversation.getLastMessagePreview()).hasSize(100);
        assertThat(conversation.getLastMessageAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        assertThat(conversation.lastReadMessageIdOf(1L)).isEqualTo(7L);
        assertThat(conversation.lastReadMessageIdOf(2L)).isZero();
    }

    @Test
    @DisplayName("읽음 위치는 앞으로만 움직인다")
    void markReadForwardOnly() {
        conversation.markRead(2L, 10L);
        conversation.markRead(2L, 5L);

        assertThat(conversation.getProviderLastReadMessageId()).isEqualTo(10L);
    }
}
