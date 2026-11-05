package com.dasisiheung.entity.space;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공간 제공자가 빌려주는 비품 한 줄 (예: 접이식 테이블 4개).
 * 보유 시설({@link Facility})은 정해진 목록에서 고르고, 비품은 제공자가 이름 · 수량을 자유롭게 적는다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment {

    public static final int MAX_NAME_LENGTH = 40;
    public static final int MAX_QUANTITY = 999;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public Equipment(String name, int quantity) {
        this.name = name.strip();
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Equipment other)) {
            return false;
        }
        return quantity == other.quantity && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, quantity);
    }
}
