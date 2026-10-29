package com.dasisiheung.service;

import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.SpaceDetailResponse;
import com.dasisiheung.dto.space.ProviderSpaceSummaryResponse;
import com.dasisiheung.dto.space.SpacePhotoResponse;
import com.dasisiheung.dto.space.SpaceProofResponse;
import com.dasisiheung.dto.space.SpaceSummaryResponse;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpacePhoto;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 저장소 주소(공개 주소·임시 주소)가 필요한 공간 응답을 만든다. */
@Component
@RequiredArgsConstructor
public class SpaceResponseMapper {

    private final FileUploadService fileUploadService;

    public ProviderSpaceResponse providerSpace(Space space) {
        return ProviderSpaceResponse.of(space, photos(space), proofs(space));
    }

    /**
     * 이용자용 상세. 지난 날짜와 이미 확정된 예약 날짜는 빼고 보여준다.
     *
     * @param bookedDates 확정 예약이 있는 날짜
     */
    public SpaceDetailResponse spaceDetail(Space space, LocalDate today, Set<LocalDate> bookedDates) {
        List<LocalDate> upcomingDates = space.getAvailableDates().stream()
                .filter(date -> !date.isBefore(today))
                .filter(date -> !bookedDates.contains(date))
                .sorted()
                .toList();
        return SpaceDetailResponse.of(space, upcomingDates, photos(space));
    }

    public SpaceSummaryResponse spaceSummary(Space space) {
        return SpaceSummaryResponse.of(space, thumbnailUrl(space));
    }

    public ProviderSpaceSummaryResponse providerSpaceSummary(Space space) {
        return ProviderSpaceSummaryResponse.of(space, thumbnailUrl(space));
    }

    public List<SpacePhotoResponse> photos(Space space) {
        return space.getPhotos().stream()
                .map(photo -> SpacePhotoResponse.of(photo, photoUrl(photo)))
                .toList();
    }

    /** 증빙은 비공개 파일이라 조회할 때마다 새 임시 주소를 발급한다. */
    public List<SpaceProofResponse> proofs(Space space) {
        return space.getProofs().stream()
                .map(proof -> SpaceProofResponse.of(proof,
                        fileUploadService.url(UploadPolicy.SPACE_PROOF, proof.getStoragePath())))
                .toList();
    }

    public String photoUrl(SpacePhoto photo) {
        return fileUploadService.url(UploadPolicy.SPACE_PHOTO, photo.getStoragePath());
    }

    /** 대표 사진(첫 번째) 주소. 사진이 없으면 null. */
    public String thumbnailUrl(Space space) {
        return space.getPhotos().stream().findFirst().map(this::photoUrl).orElse(null);
    }
}
