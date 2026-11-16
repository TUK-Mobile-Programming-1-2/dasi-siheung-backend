package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.inquiry.AdminInquiryResponse;
import com.dasisiheung.dto.inquiry.AnswerInquiryRequest;
import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.repository.InquiryPhotoRepository;
import com.dasisiheung.repository.InquiryRepository;
import com.dasisiheung.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 1:1 문의 답변. 답변하면 커밋된 뒤 문의한 회원에게 푸시를 보낸다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryPhotoRepository photoRepository;
    private final MemberRepository memberRepository;
    private final InquiryPhotoUrls photoUrls;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * 답변 대기는 오래된 순(먼저 온 문의부터), 그 밖은 최근 순.
     *
     * @param status null 이면 전체
     */
    public PageResponse<AdminInquiryResponse> getInquiries(InquiryStatus status, PageQuery pageQuery) {
        Sort.Direction direction = status == InquiryStatus.WAITING ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Inquiry> inquiries = inquiryRepository.search(status,
                pageQuery.toPageable(Sort.by(direction, "createdAt", "id")));
        Map<Long, Long> photoCounts = photoRepository.countByInquiryIds(inquiries.map(Inquiry::getId).getContent());
        return PageResponse.from(inquiries, inquiry -> AdminInquiryResponse.summary(inquiry,
                photoCounts.getOrDefault(inquiry.getId(), 0L).intValue()));
    }

    /** 사진 임시 주소(10분)를 함께 준다. */
    public AdminInquiryResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.getInquiry(inquiryId);
        return AdminInquiryResponse.detail(inquiry, photoUrls.of(inquiryId));
    }

    /** @throws com.dasisiheung.exception.BusinessException 없으면 INQUIRY_NOT_FOUND, 이미 답변했으면 INQUIRY_ALREADY_ANSWERED */
    @Transactional
    public AdminInquiryResponse answer(Long adminId, Long inquiryId, AnswerInquiryRequest request) {
        Inquiry inquiry = inquiryRepository.getInquiryWithLock(inquiryId);
        Member admin = memberRepository.getReferenceById(adminId);
        inquiry.answer(admin, request.answer(), LocalDateTime.now(clock));
        inquiryRepository.flush();

        eventPublisher.publishEvent(new InquiryAnsweredEvent(inquiry.getId(), inquiry.getMember().getId(),
                inquiry.getCategory()));
        return AdminInquiryResponse.detail(inquiry, photoUrls.of(inquiryId));
    }
}
