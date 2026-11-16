package com.dasisiheung.dto.inquiry;

import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원이 보는 1:1 문의.
 * 사진 임시 주소는 발급 비용이 있어 등록 · 사진 올리기 · 상세 응답에만 담고, 목록에는 장수만 담는다.
 */
public record InquiryResponse(
        @Schema(description = "문의 ID", example = "12") Long id,
        @Schema(description = "분류", example = "ACCOUNT") InquiryCategory category,
        @Schema(description = "문의 내용") String content,
        @Schema(description = "상태 (WAITING 답변 대기 | ANSWERED 답변 완료)", example = "WAITING") InquiryStatus status,
        @Schema(description = "운영자 답변 (답변 전 null)") String answer,
        @Schema(description = "답변 시각 (답변 전 null)") LocalDateTime answeredAt,
        @Schema(description = "첨부 사진 장수", example = "0") int photoCount,
        @Schema(description = "첨부 사진 (올린 순, 목록에서는 null)") List<InquiryPhotoResponse> photos,
        @Schema(description = "등록 시각") LocalDateTime createdAt
) {

    /** 목록용: 사진은 장수만. */
    public static InquiryResponse summary(Inquiry inquiry, int photoCount) {
        return of(inquiry, photoCount, null);
    }

    /** 상세용: 사진 임시 주소까지. */
    public static InquiryResponse detail(Inquiry inquiry, List<InquiryPhotoResponse> photos) {
        return of(inquiry, photos.size(), List.copyOf(photos));
    }

    private static InquiryResponse of(Inquiry inquiry, int photoCount, List<InquiryPhotoResponse> photos) {
        return new InquiryResponse(inquiry.getId(), inquiry.getCategory(), inquiry.getContent(), inquiry.getStatus(),
                inquiry.getAnswer(), inquiry.getAnsweredAt(), photoCount, photos, inquiry.getCreatedAt());
    }
}
