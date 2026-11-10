package com.dasisiheung.service;

import com.dasisiheung.dto.faq.FaqRequest;
import com.dasisiheung.dto.faq.FaqResponse;
import com.dasisiheung.entity.faq.Faq;
import com.dasisiheung.entity.faq.FaqCategory;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.FaqRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 자주 묻는 질문 조회(누구나) · 관리(관리자). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    private final FaqRepository faqRepository;

    /** @param category null 이면 전체 */
    public List<FaqResponse> getFaqs(FaqCategory category) {
        List<Faq> faqs = category == null
                ? faqRepository.findAllByOrderByDisplayOrderAscIdAsc()
                : faqRepository.findByCategoryOrderByDisplayOrderAscIdAsc(category);
        return faqs.stream().map(FaqResponse::from).toList();
    }

    /** 맨 뒤에 붙인다. */
    @Transactional
    public FaqResponse create(FaqRequest request) {
        int nextOrder = faqRepository.findMaxDisplayOrder() + 1;
        Faq faq = Faq.create(request.category(), request.question(), request.answer(), nextOrder);
        return FaqResponse.from(faqRepository.save(faq));
    }

    /** @throws BusinessException 없으면 FAQ_NOT_FOUND */
    @Transactional
    public FaqResponse update(Long faqId, FaqRequest request) {
        Faq faq = getFaq(faqId);
        faq.edit(request.category(), request.question(), request.answer());
        return FaqResponse.from(faq);
    }

    /** @throws BusinessException 없으면 FAQ_NOT_FOUND */
    @Transactional
    public void delete(Long faqId) {
        faqRepository.delete(getFaq(faqId));
    }

    /**
     * 받은 순서대로 0부터 다시 매긴다. 다른 관리자가 그사이 추가 · 삭제했으면 목록이 어긋나므로 거절한다.
     *
     * @param faqIds 모든 질문 ID 를 한 번씩
     * @return 바뀐 순서의 전체 목록
     * @throws BusinessException 빠졌거나 · 겹치거나 · 없는 ID 가 있으면 INVALID_FAQ_ORDER
     */
    @Transactional
    public List<FaqResponse> reorder(List<Long> faqIds) {
        Map<Long, Faq> faqsById = faqRepository.findAll().stream()
                .collect(Collectors.toMap(Faq::getId, Function.identity()));
        if (faqIds.size() != faqsById.size() || !faqsById.keySet().equals(new HashSet<>(faqIds))) {
            throw new BusinessException(ErrorCode.INVALID_FAQ_ORDER);
        }
        for (int order = 0; order < faqIds.size(); order++) {
            faqsById.get(faqIds.get(order)).moveTo(order);
        }
        return faqIds.stream().map(faqsById::get).map(FaqResponse::from).toList();
    }

    private Faq getFaq(Long faqId) {
        return faqRepository.findById(faqId).orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
    }
}
