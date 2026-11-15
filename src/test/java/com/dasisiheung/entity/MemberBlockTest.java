package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberBlock;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberBlockTest {

    private final Member me = ApplicationFixtures.applicant(1L);
    private final Member other = ApplicationFixtures.applicant(2L);

    @Test
    @DisplayName("다른 회원을 차단하면 누가 · 누구를 · 언제 차단했는지 남는다")
    void block() {
        MemberBlock block = MemberBlock.block(me, other, FixedClockConfig.FIXED_LOCAL_NOW);

        assertThat(block.getBlocker()).isSameAs(me);
        assertThat(block.getBlocked()).isSameAs(other);
        assertThat(block.getCreatedAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void cannotBlockSelf() {
        assertThatThrownBy(() -> MemberBlock.block(me, ApplicationFixtures.applicant(1L), FixedClockConfig.FIXED_LOCAL_NOW))
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_BLOCK_SELF);
    }
}
