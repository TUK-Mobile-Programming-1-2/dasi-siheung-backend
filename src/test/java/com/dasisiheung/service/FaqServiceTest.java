package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.dto.faq.FaqRequest;
import com.dasisiheung.dto.faq.FaqResponse;
import com.dasisiheung.entity.faq.Faq;
import com.dasisiheung.entity.faq.FaqCategory;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.FaqRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    private static final FaqRequest REQUEST = new FaqRequest(FaqCategory.APPLICATION, " 결제는 어떻게 하나요? ", "앱 밖에서 합니다.");

    @Mock
    private FaqRepository faqRepository;

    private FaqService faqService;
    private Faq first;
    private Faq second;
    private Faq third;

    @BeforeEach
    void setUp() {
        faqService = new FaqService(faqRepository);
        first = faq(1L, 0);
        second = faq(2L, 1);
        third = faq(3L, 2);
    }

    @Test
    @DisplayName("분류를 주면 그 분류만, 없으면 전체를 보여주는 순서대로")
    void getFaqs() {
        given(faqRepository.findAllByOrderByDisplayOrderAscIdAsc()).willReturn(List.of(first, second, third));
        given(faqRepository.findByCategoryOrderByDisplayOrderAscIdAsc(FaqCategory.ETC)).willReturn(List.of(second));

        assertThat(faqService.getFaqs(null)).extracting(FaqResponse::id).containsExactly(1L, 2L, 3L);
        assertThat(faqService.getFaqs(FaqCategory.ETC)).extracting(FaqResponse::id).containsExactly(2L);
    }

    @Test
    @DisplayName("추가하면 맨 뒤 순서 + 1 로 붙는다")
    void createAppendsToEnd() {
        given(faqRepository.findMaxDisplayOrder()).willReturn(8);
        given(faqRepository.save(any(Faq.class))).willAnswer(invocation -> invocation.getArgument(0));

        FaqResponse response = faqService.create(REQUEST);

        ArgumentCaptor<Faq> saved = ArgumentCaptor.forClass(Faq.class);
        then(faqRepository).should().save(saved.capture());
        assertThat(saved.getValue().getDisplayOrder()).isEqualTo(9);
        assertThat(response.question()).isEqualTo("결제는 어떻게 하나요?");
    }

    @Test
    @DisplayName("질문이 하나도 없으면 첫 질문은 0 번이다")
    void createFirst() {
        given(faqRepository.findMaxDisplayOrder()).willReturn(-1);
        given(faqRepository.save(any(Faq.class))).willAnswer(invocation -> invocation.getArgument(0));

        faqService.create(REQUEST);

        ArgumentCaptor<Faq> saved = ArgumentCaptor.forClass(Faq.class);
        then(faqRepository).should().save(saved.capture());
        assertThat(saved.getValue().getDisplayOrder()).isZero();
    }

    @Test
    @DisplayName("수정 · 삭제할 질문이 없으면 FAQ_NOT_FOUND")
    void notFound() {
        given(faqRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> faqService.update(99L, REQUEST))
                .extracting("errorCode").isEqualTo(ErrorCode.FAQ_NOT_FOUND);
        assertThatThrownBy(() -> faqService.delete(99L))
                .extracting("errorCode").isEqualTo(ErrorCode.FAQ_NOT_FOUND);
        then(faqRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("수정하면 순서는 그대로 두고 내용만 바꾼다")
    void update() {
        given(faqRepository.findById(2L)).willReturn(Optional.of(second));

        FaqResponse response = faqService.update(2L, REQUEST);

        assertThat(response.category()).isEqualTo(FaqCategory.APPLICATION);
        assertThat(second.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("순서 변경: 받은 순서대로 0부터 다시 매긴다")
    void reorder() {
        given(faqRepository.findAll()).willReturn(List.of(first, second, third));

        List<FaqResponse> response = faqService.reorder(List.of(3L, 1L, 2L));

        assertThat(response).extracting(FaqResponse::id).containsExactly(3L, 1L, 2L);
        assertThat(third.getDisplayOrder()).isZero();
        assertThat(first.getDisplayOrder()).isEqualTo(1);
        assertThat(second.getDisplayOrder()).isEqualTo(2);
    }

    @ParameterizedTest
    @MethodSource("invalidOrders")
    @DisplayName("순서 변경: 빠지거나 겹치거나 없는 ID 가 있으면 INVALID_FAQ_ORDER 이고 아무것도 바꾸지 않는다")
    void invalidOrder(List<Long> faqIds) {
        given(faqRepository.findAll()).willReturn(List.of(first, second, third));

        assertThatThrownBy(() -> faqService.reorder(faqIds))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FAQ_ORDER);
        assertThat(List.of(first, second, third)).extracting(Faq::getDisplayOrder).containsExactly(0, 1, 2);
    }

    static List<List<Long>> invalidOrders() {
        return List.of(
                List.of(1L, 2L),
                List.of(1L, 2L, 2L),
                List.of(1L, 2L, 3L, 4L),
                List.of(1L, 2L, 4L));
    }

    private static Faq faq(Long id, int order) {
        Faq faq = Faq.create(FaqCategory.ETC, "질문 " + id, "답변 " + id, order);
        ReflectionTestUtils.setField(faq, "id", id);
        return faq;
    }
}
