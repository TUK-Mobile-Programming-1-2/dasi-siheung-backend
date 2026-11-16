package com.dasisiheung.service;

import com.dasisiheung.dto.inquiry.InquiryPhotoResponse;
import com.dasisiheung.repository.InquiryPhotoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 문의 첨부 사진 목록을 임시 주소(10분)와 함께 만든다. 회원 · 관리자 화면이 같이 쓴다. */
@Component
@RequiredArgsConstructor
public class InquiryPhotoUrls {

    private final InquiryPhotoRepository photoRepository;
    private final FileUploadService fileUploadService;

    /** 올린 순. */
    public List<InquiryPhotoResponse> of(Long inquiryId) {
        return photoRepository.findByInquiry_IdOrderByIdAsc(inquiryId).stream()
                .map(photo -> InquiryPhotoResponse.of(photo,
                        fileUploadService.url(UploadPolicy.INQUIRY_PHOTO, photo.getStoragePath())))
                .toList();
    }
}
