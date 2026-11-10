package com.dasisiheung.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dasisiheung.entity.faq.Faq;
import com.dasisiheung.entity.faq.FaqCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaqTest {

    @Test
    @DisplayName("질문 · 답변의 앞뒤 공백은 지우고 저장한다")
    void create() {
        Faq faq = Faq.create(FaqCategory.ACCOUNT, "  질문인가요? ", "\n답변입니다.\n두 번째 줄\n", 3);

        assertThat(faq.getCategory()).isEqualTo(FaqCategory.ACCOUNT);
        assertThat(faq.getQuestion()).isEqualTo("질문인가요?");
        assertThat(faq.getAnswer()).isEqualTo("답변입니다.\n두 번째 줄");
        assertThat(faq.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("수정하면 분류 · 질문 · 답변이 바뀌고 순서는 그대로다")
    void edit() {
        Faq faq = Faq.create(FaqCategory.ACCOUNT, "질문", "답변", 2);

        faq.edit(FaqCategory.SPACE, " 새 질문 ", " 새 답변 ");

        assertThat(faq.getCategory()).isEqualTo(FaqCategory.SPACE);
        assertThat(faq.getQuestion()).isEqualTo("새 질문");
        assertThat(faq.getAnswer()).isEqualTo("새 답변");
        assertThat(faq.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("순서는 0 이상만 된다")
    void negativeOrder() {
        Faq faq = Faq.create(FaqCategory.ETC, "질문", "답변", 0);

        faq.moveTo(5);
        assertThat(faq.getDisplayOrder()).isEqualTo(5);
        assertThatThrownBy(() -> faq.moveTo(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Faq.create(FaqCategory.ETC, "질문", "답변", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
