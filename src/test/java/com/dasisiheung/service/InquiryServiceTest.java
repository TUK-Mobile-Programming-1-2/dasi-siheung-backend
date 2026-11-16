package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import com.dasisiheung.dto.inquiry.InquiryPhotoResponse;
import com.dasisiheung.dto.inquiry.InquiryRequest;
import com.dasisiheung.dto.inquiry.InquiryResponse;
import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import com.dasisiheung.entity.inquiry.InquiryPhoto;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.InquiryPhotoRepository;
import com.dasisiheung.repository.InquiryRepository;
import com.dasisiheung.repository.MemberRepository;
import com.dasisiheung.support.ApplicationFixtures;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.TestFiles;
import com.dasisiheung.util.FileType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long INQUIRY_ID = 12L;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryPhotoRepository photoRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private InquiryPhotoUrls photoUrls;

    private InquiryService service;
    private Inquiry inquiry;

    @BeforeEach
    void setUp() {
        service = new InquiryService(inquiryRepository, photoRepository, memberRepository, fileUploadService, photoUrls,
                FixedClockConfig.fixedClock());
        inquiry = new Inquiry(ApplicationFixtures.applicant(OWNER_ID), InquiryCategory.ACCOUNT, "로그인이 안 돼요.");
        ReflectionTestUtils.setField(inquiry, "id", INQUIRY_ID);
    }

    @Test
    @DisplayName("문의 등록: 답변 대기, 사진은 빈 목록으로 응답한다")
    void create() {
        Member member = ApplicationFixtures.applicant(OWNER_ID);
        given(memberRepository.getReferenceById(OWNER_ID)).willReturn(member);
        given(inquiryRepository.saveAndFlush(any(Inquiry.class))).willAnswer(invocation -> invocation.getArgument(0));

        InquiryResponse response = service.create(OWNER_ID, new InquiryRequest(InquiryCategory.ACCOUNT, " 로그인이 안 돼요. "));

        assertThat(response.status()).isEqualTo(InquiryStatus.WAITING);
        assertThat(response.content()).isEqualTo("로그인이 안 돼요.");
        assertThat(response.photos()).isEmpty();
        assertThat(response.photoCount()).isZero();
    }

    @Test
    @DisplayName("사진 올리기: 문의를 잠그고, 비공개 저장소에 올린 뒤 전체 사진을 돌려준다")
    void uploadPhotos() {
        given(inquiryRepository.getInquiryWithLock(INQUIRY_ID)).willReturn(inquiry);
        given(photoRepository.countByInquiry_Id(INQUIRY_ID)).willReturn(1);
        given(fileUploadService.upload(eq(UploadPolicy.INQUIRY_PHOTO), eq("inquiries/12/photos"), any(MultipartFile.class)))
                .willReturn(new StoredFile("inquiries/12/photos/a.jpg", FileType.JPEG, "a.jpg", 10L),
                        new StoredFile("inquiries/12/photos/b.jpg", FileType.JPEG, "b.jpg", 20L));
        given(photoUrls.of(INQUIRY_ID)).willReturn(List.of(new InquiryPhotoResponse(1L, "u1"),
                new InquiryPhotoResponse(2L, "u2"), new InquiryPhotoResponse(3L, "u3")));

        InquiryResponse response = service.uploadPhotos(OWNER_ID, INQUIRY_ID,
                List.of(TestFiles.jpeg("files"), TestFiles.jpeg("files")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InquiryPhoto>> saved = ArgumentCaptor.forClass(List.class);
        then(photoRepository).should().saveAllAndFlush(saved.capture());
        assertThat(saved.getValue()).extracting(InquiryPhoto::getStoragePath)
                .containsExactly("inquiries/12/photos/a.jpg", "inquiries/12/photos/b.jpg");
        assertThat(saved.getValue()).allSatisfy(photo -> {
            assertThat(photo.getInquiry()).isSameAs(inquiry);
            assertThat(photo.getContentType()).isEqualTo("image/jpeg");
            assertThat(photo.getCreatedAt()).isEqualTo(FixedClockConfig.FIXED_LOCAL_NOW);
        });
        assertThat(response.photoCount()).isEqualTo(3);
        assertThat(response.photos()).extracting(InquiryPhotoResponse::url).containsExactly("u1", "u2", "u3");
    }

    @Test
    @DisplayName("사진 올리기: 3장을 넘으면 파일을 올리기 전에 400 INQUIRY_PHOTO_LIMIT_EXCEEDED")
    void photoLimitBeforeUpload() {
        given(inquiryRepository.getInquiryWithLock(INQUIRY_ID)).willReturn(inquiry);
        given(photoRepository.countByInquiry_Id(INQUIRY_ID)).willReturn(2);

        assertThatThrownBy(() -> service.uploadPhotos(OWNER_ID, INQUIRY_ID,
                List.of(TestFiles.jpeg("files"), TestFiles.jpeg("files"))))
                .extracting("errorCode").isEqualTo(ErrorCode.INQUIRY_PHOTO_LIMIT_EXCEEDED);
        then(fileUploadService).shouldHaveNoInteractions();
        then(photoRepository).should(never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("사진 올리기: 남의 문의면 파일을 올리지 않고 403 NOT_INQUIRY_OWNER")
    void uploadToOthersInquiry() {
        given(inquiryRepository.getInquiryWithLock(INQUIRY_ID)).willReturn(inquiry);

        assertThatThrownBy(() -> service.uploadPhotos(2L, INQUIRY_ID, List.of(TestFiles.jpeg("files"))))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_INQUIRY_OWNER);
        then(fileUploadService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("사진 올리기: 파일이 없으면 400 FILE_REQUIRED")
    void noFiles() {
        assertThatThrownBy(() -> service.uploadPhotos(OWNER_ID, INQUIRY_ID, List.of()))
                .extracting("errorCode").isEqualTo(ErrorCode.FILE_REQUIRED);
        then(inquiryRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상세: 본인 문의면 사진 임시 주소와 함께, 남의 문의면 403")
    void getInquiry() {
        given(inquiryRepository.getInquiry(INQUIRY_ID)).willReturn(inquiry);
        given(photoUrls.of(INQUIRY_ID)).willReturn(List.of(new InquiryPhotoResponse(1L, "u1")));

        InquiryResponse response = service.getInquiry(OWNER_ID, INQUIRY_ID);

        assertThat(response.id()).isEqualTo(INQUIRY_ID);
        assertThat(response.photos()).hasSize(1);
        assertThatThrownBy(() -> service.getInquiry(2L, INQUIRY_ID))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_INQUIRY_OWNER);
        then(photoUrls).should().of(INQUIRY_ID);
        then(fileUploadService).should(never()).url(any(), anyString());
    }
}
