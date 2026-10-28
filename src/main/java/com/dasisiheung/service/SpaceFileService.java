package com.dasisiheung.service;

import com.dasisiheung.dto.space.SpacePhotoResponse;
import com.dasisiheung.dto.space.SpaceProofResponse;
import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.Space;
import com.dasisiheung.entity.space.SpacePhoto;
import com.dasisiheung.entity.space.SpaceProof;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.repository.SpaceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 공간 사진 · 권한 증빙 업로드.
 * 업로드 중 오류로 트랜잭션이 롤백되면 이미 올린 파일은 FileUploadService 가 지운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceFileService {

    private static final String PHOTO_DIRECTORY = "spaces/%d/photos";
    private static final String PROOF_DIRECTORY = "spaces/%d/proofs";

    private final SpaceRepository spaceRepository;
    private final FileUploadService fileUploadService;
    private final SpaceResponseMapper responseMapper;
    private final Clock clock;

    /** 사진을 순서대로 맨 뒤에 추가하고 전체 사진 목록을 돌려준다. */
    @Transactional
    public List<SpacePhotoResponse> addPhotos(Long providerId, Long spaceId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_REQUIRED);
        }
        Space space = getOwnedSpace(providerId, spaceId);
        space.validateCanAddPhotos(files.size());

        String directory = PHOTO_DIRECTORY.formatted(spaceId);
        for (MultipartFile file : files) {
            StoredFile stored = fileUploadService.upload(UploadPolicy.SPACE_PHOTO, directory, file);
            space.addPhoto(stored.path(), now());
        }
        spaceRepository.flush();
        return responseMapper.photos(space);
    }

    @Transactional
    public void deletePhoto(Long providerId, Long spaceId, Long photoId) {
        Space space = getOwnedSpace(providerId, spaceId);
        SpacePhoto removed = space.removePhoto(photoId);
        fileUploadService.deleteAfterCommit(UploadPolicy.SPACE_PHOTO, removed.getStoragePath());
    }

    @Transactional
    public List<SpacePhotoResponse> reorderPhotos(Long providerId, Long spaceId, List<Long> photoIds) {
        Space space = getOwnedSpace(providerId, spaceId);
        space.reorderPhotos(photoIds);
        return responseMapper.photos(space);
    }

    /** 공개·일시 중단 중인 공간에 증빙을 추가하면 다시 검수 대기가 된다. */
    @Transactional
    public SpaceProofResponse addProof(Long providerId, Long spaceId, ProofType proofType, MultipartFile file) {
        Space space = getOwnedSpace(providerId, spaceId);
        if (space.getProofs().size() >= Space.MAX_PROOFS) {
            throw new BusinessException(ErrorCode.SPACE_PROOF_LIMIT_EXCEEDED);
        }
        StoredFile stored = fileUploadService.upload(UploadPolicy.SPACE_PROOF, PROOF_DIRECTORY.formatted(spaceId), file);
        SpaceProof proof = space.addProof(proofType, new SpaceProof.StoredProofFile(stored.path(),
                stored.originalName(), stored.type().contentType(), stored.size()), now());
        spaceRepository.flush();
        return SpaceProofResponse.of(proof, fileUploadService.url(UploadPolicy.SPACE_PROOF, proof.getStoragePath()));
    }

    public List<SpaceProofResponse> getProofs(Long providerId, Long spaceId) {
        return responseMapper.proofs(getOwnedSpace(providerId, spaceId));
    }

    @Transactional
    public void deleteProof(Long providerId, Long spaceId, Long proofId) {
        Space space = getOwnedSpace(providerId, spaceId);
        SpaceProof removed = space.removeProof(proofId);
        fileUploadService.deleteAfterCommit(UploadPolicy.SPACE_PROOF, removed.getStoragePath());
    }

    private Space getOwnedSpace(Long providerId, Long spaceId) {
        Space space = spaceRepository.getSpace(spaceId);
        space.validateOwner(providerId);
        return space;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
