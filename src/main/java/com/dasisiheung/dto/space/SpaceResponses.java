package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.Space;
import java.util.Collection;
import java.util.List;

/** 공간 응답 DTO 들의 공통 변환. 집합은 응답마다 순서가 같도록 정렬해 내려준다. */
final class SpaceResponses {

    private SpaceResponses() {
    }

    static <T extends Comparable<? super T>> List<T> sorted(Collection<T> values) {
        return values.stream().sorted().toList();
    }

    /** 비품은 제공자가 적은 순서 그대로 내려준다. */
    static List<EquipmentItem> equipment(Space space) {
        return space.getEquipment().stream().map(EquipmentItem::from).toList();
    }
}
