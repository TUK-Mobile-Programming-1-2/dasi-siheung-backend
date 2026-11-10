package com.dasisiheung.entity.faq;

import com.dasisiheung.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자주 묻는 질문 한 건. 관리자가 쓰고, 누구나 {@code displayOrder} 순으로 본다. */
@Getter
@Entity
@Table(name = "faq")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseTimeEntity {

    public static final int MAX_QUESTION_LENGTH = 200;
    public static final int MAX_ANSWER_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FaqCategory category;

    @Column(nullable = false, length = MAX_QUESTION_LENGTH)
    private String question;

    @Column(nullable = false, length = MAX_ANSWER_LENGTH)
    private String answer;

    /** 작을수록 위. 같으면 먼저 만든 것(id)이 위다. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private Faq(FaqCategory category, String question, String answer, int displayOrder) {
        apply(category, question, answer);
        this.displayOrder = requireValidOrder(displayOrder);
    }

    /** @param displayOrder 보통 지금 맨 뒤 순서 + 1 */
    public static Faq create(FaqCategory category, String question, String answer, int displayOrder) {
        return new Faq(category, question, answer, displayOrder);
    }

    /** 분류 · 질문 · 답변을 모두 바꾼다. 순서는 그대로다. */
    public void edit(FaqCategory category, String question, String answer) {
        apply(category, question, answer);
    }

    /** @throws IllegalArgumentException 순서가 음수일 때 (서비스가 0부터 매기므로 프로그래밍 오류) */
    public void moveTo(int displayOrder) {
        this.displayOrder = requireValidOrder(displayOrder);
    }

    private void apply(FaqCategory category, String question, String answer) {
        this.category = category;
        this.question = question.strip();
        this.answer = answer.strip();
    }

    private static int requireValidOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("순서는 0 이상이어야 합니다: " + displayOrder);
        }
        return displayOrder;
    }
}
