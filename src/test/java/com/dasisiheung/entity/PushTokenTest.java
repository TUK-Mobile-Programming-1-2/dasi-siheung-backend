package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.PushToken;
import com.dasisiheung.entity.member.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PushTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 20, 0);

    private final Member user = Member.signUp("user@example.com", "hash", "김시흥", null, Role.USER);
    private final Member other = Member.signUp("other@example.com", "hash", "이배곧", null, Role.PROVIDER);

    @Test
    @DisplayName("처음 등록하면 만든 시각과 마지막 등록 시각이 같다")
    void register() {
        PushToken token = PushToken.register(user, "token-1", NOW);

        assertThat(token.getMember()).isSameAs(user);
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getLastRegisteredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("다시 등록하면 마지막 등록 시각이 바뀌고, 다른 계정이면 그 회원에게 옮긴다")
    void renew() {
        PushToken token = PushToken.register(user, "token-1", NOW);

        token.renew(other, NOW.plusDays(3));

        assertThat(token.getMember()).isSameAs(other);
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getLastRegisteredAt()).isEqualTo(NOW.plusDays(3));
    }
}
