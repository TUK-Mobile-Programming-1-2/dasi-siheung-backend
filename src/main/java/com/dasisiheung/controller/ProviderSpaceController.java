package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.space.AvailableDatesRequest;
import com.dasisiheung.dto.space.PhotoOrderRequest;
import com.dasisiheung.dto.space.ProviderSpaceResponse;
import com.dasisiheung.dto.space.ProviderSpaceSummaryResponse;
import com.dasisiheung.dto.space.SpacePhotoResponse;
import com.dasisiheung.dto.space.SpaceProofResponse;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.SpaceStatus;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.SpaceFileService;
import com.dasisiheung.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 공간 제공자(PROVIDER) 전용. /api/v1/provider/** 는 SecurityConfig 에서 역할을 검사한다. */
@Tag(name = "Provider Space", description = "공간 제공자 — 내 공간 등록 · 관리")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/provider/spaces")
@RequiredArgsConstructor
public class ProviderSpaceController {

    private final SpaceService spaceService;
    private final SpaceFileService spaceFileService;

    @Operation(summary = "공간 등록",
            description = "지역은 좌표로 판정합니다. 등록하면 관리자 검수 대기(PENDING_REVIEW) 상태가 되고, 사진·증빙은 등록 후 따로 올립니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<ProviderSpaceResponse> register(@LoginMember AuthMember loginMember,
                                                     @Valid @RequestBody SpaceRequest request) {
        return ApiResult.ok(spaceService.register(loginMember.id(), request));
    }

    @Operation(summary = "내 공간 목록", description = "검수 상태와 사유를 함께 보여줍니다. status 로 상태별 필터 (생략하면 전체).")
    @GetMapping
    public ApiResult<PageResponse<ProviderSpaceSummaryResponse>> getMySpaces(
            @LoginMember AuthMember loginMember,
            @RequestParam(required = false) SpaceStatus status,
            @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(spaceService.getMySpaces(loginMember.id(), status, pageQuery));
    }

    @Operation(summary = "내 공간 상세", description = "사진·증빙(임시 주소)·검수 사유를 포함합니다.")
    @GetMapping("/{spaceId}")
    public ApiResult<ProviderSpaceResponse> getMySpace(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(spaceService.getMySpace(loginMember.id(), spaceId));
    }

    @Operation(summary = "공간 정보 수정",
            description = "공개·일시 중단 중에 주소·건물·호실·면적·용도(검수 항목)를 바꾸면 다시 검수 대기가 됩니다. "
                    + "availableDates 를 보내지 않으면 캘린더는 그대로입니다.")
    @PutMapping("/{spaceId}")
    public ApiResult<ProviderSpaceResponse> update(@LoginMember AuthMember loginMember, @PathVariable Long spaceId,
                                                   @Valid @RequestBody SpaceRequest request) {
        return ApiResult.ok(spaceService.update(loginMember.id(), spaceId, request));
    }

    @Operation(summary = "사용 가능 날짜(캘린더) 설정", description = "보낸 날짜 목록으로 통째로 바꿉니다. 검수 상태에는 영향이 없습니다.")
    @PutMapping("/{spaceId}/available-dates")
    public ApiResult<ProviderSpaceResponse> changeAvailableDates(@LoginMember AuthMember loginMember,
                                                                 @PathVariable Long spaceId,
                                                                 @Valid @RequestBody AvailableDatesRequest request) {
        return ApiResult.ok(spaceService.changeAvailableDates(loginMember.id(), spaceId, request.dates()));
    }

    @Operation(summary = "공개 일시 중단", description = "공개(PUBLISHED) 중인 공간만 중단할 수 있습니다. 새 신청을 받지 않습니다.")
    @PostMapping("/{spaceId}/pause")
    public ApiResult<ProviderSpaceResponse> pause(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(spaceService.pause(loginMember.id(), spaceId));
    }

    @Operation(summary = "공개 재개", description = "일시 중단(PAUSED)한 공간을 다시 공개합니다.")
    @PostMapping("/{spaceId}/resume")
    public ApiResult<ProviderSpaceResponse> resume(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(spaceService.resume(loginMember.id(), spaceId));
    }

    @Operation(summary = "다시 검수 요청", description = "수정 요청(REVISION_REQUESTED)·공개 보류(ON_HOLD)를 받은 공간을 고친 뒤 검수를 다시 요청합니다.")
    @PostMapping("/{spaceId}/resubmit")
    public ApiResult<ProviderSpaceResponse> resubmit(@LoginMember AuthMember loginMember, @PathVariable Long spaceId) {
        return ApiResult.ok(spaceService.resubmit(loginMember.id(), spaceId));
    }

    @Operation(summary = "사진 추가", description = "여러 장을 한 번에 올립니다. (multipart 파트 이름: files, 공간당 최대 10장, JPG·PNG·WEBP 10MB)")
    @PostMapping(path = "/{spaceId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<List<SpacePhotoResponse>> addPhotos(@LoginMember AuthMember loginMember,
                                                         @PathVariable Long spaceId,
                                                         @RequestPart("files") List<MultipartFile> files) {
        return ApiResult.ok(spaceFileService.addPhotos(loginMember.id(), spaceId, files));
    }

    @Operation(summary = "사진 순서 변경", description = "모든 사진 ID 를 원하는 순서대로 보냅니다. 첫 번째가 대표 사진입니다.")
    @PutMapping("/{spaceId}/photos/order")
    public ApiResult<List<SpacePhotoResponse>> reorderPhotos(@LoginMember AuthMember loginMember,
                                                             @PathVariable Long spaceId,
                                                             @Valid @RequestBody PhotoOrderRequest request) {
        return ApiResult.ok(spaceFileService.reorderPhotos(loginMember.id(), spaceId, request.photoIds()));
    }

    @Operation(summary = "사진 삭제")
    @DeleteMapping("/{spaceId}/photos/{photoId}")
    public ApiResult<Void> deletePhoto(@LoginMember AuthMember loginMember,
                                       @PathVariable Long spaceId, @PathVariable Long photoId) {
        spaceFileService.deletePhoto(loginMember.id(), spaceId, photoId);
        return ApiResult.ok();
    }

    @Operation(summary = "권한 증빙 추가",
            description = "multipart 파트: file(파일), proofType(증빙 종류 텍스트). PDF·JPG·PNG 20MB, 공간당 최대 5개. "
                    + "공개·일시 중단 중인 공간은 다시 검수 대기가 됩니다.")
    @PostMapping(path = "/{spaceId}/proofs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<SpaceProofResponse> addProof(@LoginMember AuthMember loginMember,
                                                  @PathVariable Long spaceId,
                                                  @RequestParam ProofType proofType,
                                                  @RequestPart("file") MultipartFile file) {
        return ApiResult.ok(spaceFileService.addProof(loginMember.id(), spaceId, proofType, file));
    }

    @Operation(summary = "권한 증빙 목록", description = "열람용 임시 주소는 10분 뒤 만료되므로 화면을 열 때마다 조회하세요.")
    @GetMapping("/{spaceId}/proofs")
    public ApiResult<List<SpaceProofResponse>> getProofs(@LoginMember AuthMember loginMember,
                                                         @PathVariable Long spaceId) {
        return ApiResult.ok(spaceFileService.getProofs(loginMember.id(), spaceId));
    }

    @Operation(summary = "권한 증빙 삭제", description = "공개·일시 중단 중인 공간은 다시 검수 대기가 됩니다.")
    @DeleteMapping("/{spaceId}/proofs/{proofId}")
    public ApiResult<Void> deleteProof(@LoginMember AuthMember loginMember,
                                       @PathVariable Long spaceId, @PathVariable Long proofId) {
        spaceFileService.deleteProof(loginMember.id(), spaceId, proofId);
        return ApiResult.ok();
    }
}
