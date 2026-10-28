package com.dasisiheung.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.SpaceRepository;
import com.dasisiheung.support.FixedClockConfig;
import com.dasisiheung.support.SpaceFixtures;
import com.dasisiheung.support.TestFiles;
import com.dasisiheung.util.FileType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class SpaceFileServiceTest {

    private static final long PROVIDER_ID = 2L;
    private static final long SPACE_ID = 10L;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private FileUploadService fileUploadService;

    private SpaceFileService service;
    private Space space;

    @BeforeEach
    void setUp() {
        service = new SpaceFileService(spaceRepository, fileUploadService, new SpaceResponseMapper(fileUploadService),
                FixedClockConfig.fixedClock());
        space = SpaceFixtures.space(SPACE_ID, SpaceFixtures.provider(PROVIDER_ID), Set.of(), FixedClockConfig.FIXED_TODAY);
        given(spaceRepository.getSpace(SPACE_ID)).willReturn(space);
    }

    @Test
    @DisplayName("사진을 spaces/{id}/photos 폴더에 올리고 순서대로 추가한다")
    void addPhotos() {
        given(fileUploadService.upload(eq(UploadPolicy.SPACE_PHOTO), eq("spaces/10/photos"), any()))
                .willReturn(new StoredFile("spaces/10/photos/a.jpg", FileType.JPEG, "a.jpg", 10),
                        new StoredFile("spaces/10/photos/b.jpg", FileType.JPEG, "b.jpg", 10));
        given(fileUploadService.url(eq(UploadPolicy.SPACE_PHOTO), anyString())).willAnswer(i -> "https://cdn/" + i.getArgument(1));

        var photos = service.addPhotos(PROVIDER_ID, SPACE_ID,
                List.<MultipartFile>of(TestFiles.jpeg("files"), TestFiles.jpeg("files")));

        assertThat(photos).extracting("url").containsExactly("https://cdn/spaces/10/photos/a.jpg", "https://cdn/spaces/10/photos/b.jpg");
        assertThat(photos).extracting("sortOrder").containsExactly(0, 1);
    }

    @Test
    @DisplayName("한도를 넘는 사진은 하나도 올리지 않고 거부한다")
    void rejectsBeforeUploadWhenOverLimit() {
        for (int i = 0; i < Space.MAX_PHOTOS - 1; i++) {
            space.addPhoto("p" + i + ".jpg", FixedClockConfig.FIXED_LOCAL_NOW);
        }

        assertThatThrownBy(() -> service.addPhotos(PROVIDER_ID, SPACE_ID,
                List.<MultipartFile>of(TestFiles.jpeg("files"), TestFiles.jpeg("files"))))
                .extracting("errorCode").isEqualTo(ErrorCode.SPACE_PHOTO_LIMIT_EXCEEDED);
        then(fileUploadService).should(never()).upload(any(), anyString(), any());
    }

    @Test
    @DisplayName("다른 제공자의 공간에는 올릴 수 없다")
    void notOwner() {
        assertThatThrownBy(() -> service.addPhotos(99L, SPACE_ID, List.<MultipartFile>of(TestFiles.jpeg("files"))))
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_SPACE_OWNER);
    }

    @Test
    @DisplayName("사진을 지우면 저장소 파일은 커밋 후 삭제하도록 예약한다")
    void deletePhoto() {
        ReflectionTestUtils.setField(space.addPhoto("spaces/10/photos/a.jpg", FixedClockConfig.FIXED_LOCAL_NOW), "id", 1L);

        service.deletePhoto(PROVIDER_ID, SPACE_ID, 1L);

        assertThat(space.getPhotos()).isEmpty();
        then(fileUploadService).should().deleteAfterCommit(UploadPolicy.SPACE_PHOTO, "spaces/10/photos/a.jpg");
    }

    @Test
    @DisplayName("증빙은 비공개 정책으로 올리고 임시 주소를 돌려준다")
    void addProof() {
        given(fileUploadService.upload(eq(UploadPolicy.SPACE_PROOF), eq("spaces/10/proofs"), any()))
                .willReturn(new StoredFile("spaces/10/proofs/c.pdf", FileType.PDF, "계약서.pdf", 99));
        given(fileUploadService.url(UploadPolicy.SPACE_PROOF, "spaces/10/proofs/c.pdf")).willReturn("https://signed");

        var proof = service.addProof(PROVIDER_ID, SPACE_ID, ProofType.LEASE_CONTRACT, TestFiles.pdf("file"));

        assertThat(proof.url()).isEqualTo("https://signed");
        assertThat(proof.contentType()).isEqualTo("application/pdf");
        assertThat(proof.originalName()).isEqualTo("계약서.pdf");
        assertThat(space.getProofs()).hasSize(1);
    }
}
