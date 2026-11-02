package com.dasisiheung.service;

import com.dasisiheung.dto.application.ActivityPhotoResponse;
import com.dasisiheung.entity.application.ActivityPhoto;
import com.dasisiheung.entity.application.Application;
import com.dasisiheung.entity.application.PhotoPhase;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.ActivityPhotoRepository;
import com.dasisiheung.repository.ApplicationRepository;
import com.dasisiheung.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 확정 예약의 활동 전후 사진. 비공개 저장소에 두고 볼 때마다 임시 주소를 발급한다.
 * 업로드 중 오류로 롤백되면 이미 올린 파일은 FileUploadService 가 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityPhotoService {

    private static final String DIRECTORY = "applications/%d/activity";

    private final ActivityPhotoRepository photoRepository;
    private final ApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;
    private final FileUploadService fileUploadService;
    private final Clock clock;

    @Transactional
    public List<ActivityPhotoResponse> upload(Long memberId, Long applicationId, PhotoPhase phase,
                                              List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_REQUIRED);
        }
        Application application = applicationRepository.getApplication(applicationId);
        int currentCount = photoRepository.countByApplication_IdAndPhase(applicationId, phase);
        ActivityPhoto.validateUpload(application, memberId, phase, currentCount, files.size(), LocalDate.now(clock));

        Member uploader = memberRepository.getReferenceById(memberId);
        LocalDateTime now = LocalDateTime.now(clock);
        for (MultipartFile file : files) {
            StoredFile stored = fileUploadService.upload(UploadPolicy.ACTIVITY_PHOTO, DIRECTORY.formatted(applicationId), file);
            photoRepository.save(ActivityPhoto.of(application, uploader, phase, new ActivityPhoto.StoredPhotoFile(
                    stored.path(), stored.originalName(), stored.type().contentType(), stored.size()), now));
        }
        photoRepository.flush();
        return toResponses(applicationId);
    }

    /** 신청자와 공간 제공자만 볼 수 있다. */
    public List<ActivityPhotoResponse> getPhotos(Long memberId, Long applicationId) {
        applicationRepository.getApplication(applicationId).validateParticipant(memberId);
        return toResponses(applicationId);
    }

    /** 올린 사람만 지울 수 있다. 파일은 커밋 뒤 저장소에서 지운다. */
    @Transactional
    public void delete(Long memberId, Long applicationId, Long photoId) {
        ActivityPhoto photo = photoRepository.findByIdAndApplication_Id(photoId, applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACTIVITY_PHOTO_NOT_FOUND));
        photo.validateUploader(memberId);
        photoRepository.delete(photo);
        fileUploadService.deleteAfterCommit(UploadPolicy.ACTIVITY_PHOTO, photo.getStoragePath());
    }

    private List<ActivityPhotoResponse> toResponses(Long applicationId) {
        return photoRepository.findByApplication_IdOrderByIdAsc(applicationId).stream()
                .sorted(Comparator.comparing(ActivityPhoto::getPhase))
                .map(photo -> ActivityPhotoResponse.of(photo,
                        fileUploadService.url(UploadPolicy.ACTIVITY_PHOTO, photo.getStoragePath())))
                .toList();
    }
}
