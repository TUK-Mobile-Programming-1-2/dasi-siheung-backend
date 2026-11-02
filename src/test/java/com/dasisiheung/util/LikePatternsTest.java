package com.dasisiheung.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LikePatternsTest {

    @Test
    @DisplayName("소문자로 바꾸고 % _ \\ 를 이스케이프해 앞뒤에 % 를 붙인다")
    void containsIgnoreCase() {
        assertThat(LikePatterns.containsIgnoreCase("Hello")).isEqualTo("%hello%");
        assertThat(LikePatterns.containsIgnoreCase("50%_off\\")).isEqualTo("%50\\%\\_off\\\\%");
    }
}
