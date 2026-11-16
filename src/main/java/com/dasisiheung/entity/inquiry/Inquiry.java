package com.dasisiheung.entity.inquiry;

import com.dasisiheung.entity.BaseTimeEntity;
import com.dasisiheung.entity.member.Member;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 1:1 문의 한 건. 회원이 남기고 관리자가 한 번 답변한다. 사진은 등록한 뒤 따로 올린다. */
@Getter
@Entity
@Table(name = "inquiry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    /** 문의 내용 최대 글자 수. (앱 입력칸과 같다) */
    public static final int MAX_CONTENT_LENGTH = 1000;
    /** 관리자 답변 최대 글자 수. */
    public static final int MAX_ANSWER_LENGTH = 2000;
    /** 문의 한 건에 붙일 수 있는 사진 최대 장수. */
    public static final int MAX_PHOTOS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(length = MAX_ANSWER_LENGTH)
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private Member answeredBy;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    public Inquiry(Member member, InquiryCategory category, String content) {
        this.member = member;
        this.category = category;
        this.content = content.strip();
        this.status = InquiryStatus.WAITING;
    }

    /** @throws BusinessException 문의한 회원이 아니면 NOT_INQUIRY_OWNER */
    public void validateOwner(Long memberId) {
        if (!member.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_INQUIRY_OWNER);
        }
    }

    /**
     * 사진을 올리기 전에 검사한다. (파일 업로드 전에 불러 실패 시 파일이 남지 않게 한다)
     *
     * @param currentCount 이미 올린 사진 수
     * @param addingCount  이번에 올릴 사진 수
     * @throws BusinessException 문의한 회원이 아니면 NOT_INQUIRY_OWNER, 답변이 끝났으면 INQUIRY_ALREADY_ANSWERED,
     *                           합쳐서 3장을 넘으면 INQUIRY_PHOTO_LIMIT_EXCEEDED
     */
    public void validatePhotoUpload(Long memberId, int currentCount, int addingCount) {
        validateOwner(memberId);
        if (isAnswered()) {
            throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        }
        if (currentCount + addingCount > MAX_PHOTOS) {
            throw new BusinessException(ErrorCode.INQUIRY_PHOTO_LIMIT_EXCEEDED);
        }
    }

    /**
     * 관리자 답변을 남긴다. 답변은 한 번만 할 수 있다.
     *
     * @throws BusinessException 이미 답변했으면 INQUIRY_ALREADY_ANSWERED
     */
    public void answer(Member admin, String answer, LocalDateTime now) {
        if (isAnswered()) {
            throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        }
        this.answer = answer.strip();
        this.answeredBy = admin;
        this.answeredAt = now;
        this.status = InquiryStatus.ANSWERED;
    }

    public boolean isAnswered() {
        return status == InquiryStatus.ANSWERED;
    }
}
