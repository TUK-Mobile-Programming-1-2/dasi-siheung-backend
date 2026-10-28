package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.ProofType;
import com.dasisiheung.entity.space.SpaceProof;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SpaceProofResponse(
        @Schema(description = "증빙 ID", example = "1") Long id,
        @Schema(description = "증빙 종류", example = "LEASE_CONTRACT") ProofType proofType,
        @Schema(description = "올린 파일 이름", example = "임대차계약서.pdf") String originalName,
        @Schema(description = "파일 형식", example = "application/pdf") String contentType,
        @Schema(description = "파일 크기(바이트)", example = "204800") long fileSize,
        @Schema(description = "열람용 임시 주소 (10분 뒤 만료)") String url,
        @Schema(description = "올린 시각") LocalDateTime createdAt
) {

    public static SpaceProofResponse of(SpaceProof proof, String url) {
        return new SpaceProofResponse(proof.getId(), proof.getProofType(), proof.getOriginalName(),
                proof.getContentType(), proof.getFileSize(), url, proof.getCreatedAt());
    }
}
