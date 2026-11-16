package com.dasisiheung.dto.inquiry;

import com.dasisiheung.entity.inquiry.Inquiry;
import com.dasisiheung.entity.inquiry.InquiryCategory;
import com.dasisiheung.entity.inquiry.InquiryStatus;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.entity.member.MemberStatus;
import com.dasisiheung.entity.member.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자가 보는 1:1 문의 (문의한 회원 정보 포함).
 * 사진 임시 주소는 상세 · 답변 응답에만 담고, 목록에는 장수만 담는다.
 */
public record AdminInquiryResponse(
        @Schema(description = "문의 ID", example = "12") Long id,
        @Schema(description = "문의한 회원 ID", example = "1") Long memberId,
        @Schema(description = "문의한 회원 이름") String memberName,
        @Schema(description = "문의한 회원 역할 (USER 이용자 | PROVIDER 공간 제공자)", example = "USER") Role memberRole,
        @Schema(description = "문의한 회원 상태", example = "ACTIVE") MemberStatus memberStatus,
        @Schema(description = "분류", example = "ACCOUNT") InquiryCategory category,
        @Schema(description = "문의 내용") String content,
        @Schema(description = "상태", example = "WAITING") InquiryStatus status,
        @Schema(description = "운영자 답변 (답변 전 null)") String answer,
        @Schema(description = "답변 시각 (답변 전 null)") LocalDateTime answeredAt,
        @Schema(description = "첨부 사진 장수", example = "2") int photoCount,
        @Schema(description = "첨부 사진 (상세 · 답변 응답에만, 목록에서는 null)") List<InquiryPhotoResponse> photos,
        @Schema(description = "등록 시각") LocalDateTime createdAt
) {

    /** 목록용: 사진은 장수만. */
    public static AdminInquiryResponse summary(Inquiry inquiry, int photoCount) {
        return of(inquiry, photoCount, null);
    }

    /** 상세용: 사진 임시 주소까지. */
    public static AdminInquiryResponse detail(Inquiry inquiry, List<InquiryPhotoResponse> photos) {
        return of(inquiry, photos.size(), List.copyOf(photos));
    }

    private static AdminInquiryResponse of(Inquiry inquiry, int photoCount, List<InquiryPhotoResponse> photos) {
        Member member = inquiry.getMember();
        return new AdminInquiryResponse(inquiry.getId(), member.getId(), member.getName(), member.getRole(),
                member.getStatus(), inquiry.getCategory(), inquiry.getContent(), inquiry.getStatus(),
                inquiry.getAnswer(), inquiry.getAnsweredAt(), photoCount, photos, inquiry.getCreatedAt());
    }
}
