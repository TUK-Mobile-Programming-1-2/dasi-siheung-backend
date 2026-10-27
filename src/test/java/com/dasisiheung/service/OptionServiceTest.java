package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.dto.option.OptionItem;
import com.dasisiheung.dto.option.OptionsResponse;
import com.dasisiheung.entity.space.CommercialTheme;
import com.dasisiheung.entity.space.UsePurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptionServiceTest {

    private final OptionsResponse options = new OptionService().getOptions();

    @Test
    @DisplayName("가입 역할 선택지에 관리자는 없다")
    void signupRolesExcludeAdmin() {
        assertThat(options.signupRoles()).extracting(OptionItem::code).containsExactly("USER", "PROVIDER");
    }

    @Test
    @DisplayName("지역은 정왕동 · 거북섬 · 배곧 순서다")
    void regions() {
        assertThat(options.regions()).extracting(OptionItem::label).containsExactly("정왕동", "거북섬", "배곧");
    }

    @Test
    @DisplayName("모든 이용 목적은 어울리는 상권 테마가 하나 이상 있다")
    void everyPurposeHasThemes() {
        for (UsePurpose purpose : UsePurpose.values()) {
            assertThat(purpose.getSuitableThemes()).as(purpose.name()).isNotEmpty();
        }
        assertThat(UsePurpose.SALES.suits(CommercialTheme.FOOD_ALLEY)).isTrue();
        assertThat(UsePurpose.SALES.suits(null)).isFalse();
    }
}
