package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.application.ActivityPhotoResponse;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.entity.application.PhotoPhase;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.ActivityPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 확정 예약의 활동 전후 사진. 신청자와 공간 제공자가 함께 쓴다. */
@Tag(name = "Activity Photo", description = "활동 전후 사진 (원상 복구 확인)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/applications/{applicationId}/activity-photos")
@RequiredArgsConstructor
public class ActivityPhotoController {

    private final ActivityPhotoService activityPhotoService;

    @Operation(summary = "활동 사진 올리기",
            description = "multipart 파트: files(사진 여러 장), phase(BEFORE · AFTER). 이용 전은 시작 하루 전~종료일, "
                    + "이용 후는 시작일~종료 7일 뒤까지. 시점별 최대 10장, JPG·PNG·WEBP 10MB.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<List<ActivityPhotoResponse>> upload(@LoginMember AuthMember loginMember,
                                                         @PathVariable Long applicationId,
                                                         @RequestParam PhotoPhase phase,
                                                         @RequestPart("files") List<MultipartFile> files) {
        return ApiResult.ok(activityPhotoService.upload(loginMember.id(), applicationId, phase, files));
    }

    @Operation(summary = "활동 사진 목록", description = "이용 전 → 이용 후, 올린 순. 임시 주소는 10분 뒤 만료됩니다.")
    @GetMapping
    public ApiResult<List<ActivityPhotoResponse>> getPhotos(@LoginMember AuthMember loginMember,
                                                            @PathVariable Long applicationId) {
        return ApiResult.ok(activityPhotoService.getPhotos(loginMember.id(), applicationId));
    }

    @Operation(summary = "활동 사진 삭제", description = "본인이 올린 사진만 지울 수 있습니다.")
    @DeleteMapping("/{photoId}")
    public ApiResult<Void> delete(@LoginMember AuthMember loginMember, @PathVariable Long applicationId,
                                  @PathVariable Long photoId) {
        activityPhotoService.delete(loginMember.id(), applicationId, photoId);
        return ApiResult.ok();
    }
}
