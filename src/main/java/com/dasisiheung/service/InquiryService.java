package com.dasisiheung.service;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.inquiry.InquiryRequest;
import com.dasisiheung.dto.inquiry.InquiryResponse;
import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryPhoto;
import com.dasisiheung.entity.inquiry.InquiryPhoto.StoredPhotoFile;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.InquiryPhotoRepository;
import com.dasisiheung.repository.InquiryRepository;
import com.dasisiheung.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원의 1:1 문의. 문의를 먼저 등록하고 사진은 따로 올린다.
 * 사진은 비공개 저장소에 두고, 문의한 회원과 관리자만 임시 주소로 본다.
 * 사진 하나라도 실패하면 그 요청의 사진은 모두 롤백되고, 이미 올린 파일은 FileUploadService 가 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private static final String PHOTO_DIRECTORY = "inquiries/%d/photos";

    private final InquiryRepository inquiryRepository;
    private final InquiryPhotoRepository photoRepository;
    private final MemberRepository memberRepository;
    private final FileUploadService fileUploadService;
    private final InquiryPhotoUrls photoUrls;
    private final Clock clock;

    @Transactional
    public InquiryResponse create(Long memberId, InquiryRequest request) {
        Member member = memberRepository.getReferenceById(memberId);
        Inquiry inquiry = inquiryRepository.saveAndFlush(new Inquiry(member, request.category(), request.content()));
        return InquiryResponse.detail(inquiry, List.of());
    }

    /**
     * 사진을 올린다. 같은 문의에 동시에 올려도 3장을 넘지 않도록 문의 행을 잠근다.
     * 응답은 이 문의의 전체 사진이다.
     *
     * @throws BusinessException 파일이 없으면 FILE_REQUIRED, 문의가 없으면 INQUIRY_NOT_FOUND,
     *                           남의 문의면 NOT_INQUIRY_OWNER, 답변이 끝났으면 INQUIRY_ALREADY_ANSWERED,
     *                           합쳐서 3장을 넘으면 INQUIRY_PHOTO_LIMIT_EXCEEDED,
     *                           사진 형식 · 크기가 맞지 않으면 UNSUPPORTED_FILE_TYPE · FILE_TOO_LARGE
     */
    @Transactional
    public InquiryResponse uploadPhotos(Long memberId, Long inquiryId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_REQUIRED);
        }
        Inquiry inquiry = inquiryRepository.getInquiryWithLock(inquiryId);
        inquiry.validatePhotoUpload(memberId, photoRepository.countByInquiry_Id(inquiryId), files.size());

        String directory = PHOTO_DIRECTORY.formatted(inquiryId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<InquiryPhoto> photos = files.stream().map(file -> {
            StoredFile stored = fileUploadService.upload(UploadPolicy.INQUIRY_PHOTO, directory, file);
            return new InquiryPhoto(inquiry, new StoredPhotoFile(stored.path(), stored.originalName(),
                    stored.type().contentType(), stored.size()), now);
        }).toList();
        photoRepository.saveAllAndFlush(photos);
        return InquiryResponse.detail(inquiry, photoUrls.of(inquiryId));
    }

    /** 내가 남긴 문의, 최근 순. 사진은 장수만. */
    public PageResponse<InquiryResponse> getMyInquiries(Long memberId, PageQuery pageQuery) {
        Page<Inquiry> inquiries = inquiryRepository.findByMember_Id(memberId,
                pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        Map<Long, Long> photoCounts = photoRepository.countByInquiryIds(inquiries.map(Inquiry::getId).getContent());
        return PageResponse.from(inquiries, inquiry -> InquiryResponse.summary(inquiry,
                photoCounts.getOrDefault(inquiry.getId(), 0L).intValue()));
    }

    /**
     * 문의 한 건. 사진 임시 주소(10분)를 함께 준다.
     *
     * @throws BusinessException 없으면 INQUIRY_NOT_FOUND, 남의 문의면 NOT_INQUIRY_OWNER
     */
    public InquiryResponse getInquiry(Long memberId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.getInquiry(inquiryId);
        inquiry.validateOwner(memberId);
        return InquiryResponse.detail(inquiry, photoUrls.of(inquiryId));
    }
}
