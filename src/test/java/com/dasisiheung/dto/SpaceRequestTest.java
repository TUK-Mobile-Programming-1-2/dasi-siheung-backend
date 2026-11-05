package com.dasisiheung.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.dto.space.EquipmentItem;
import com.dasisiheung.dto.space.SpaceRequest;
import com.dasisiheung.entity.space.Equipment;
import com.dasisiheung.entity.space.SpaceDetails;
import com.dasisiheung.support.SpaceFixtures;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpaceRequestTest {

    @Test
    @DisplayName("비품 이름은 앞뒤 공백 · 대소문자를 무시하고 겹치면 안 된다")
    void equipmentNamesUnique() {
        assertThat(withEquipment(List.of(new EquipmentItem("의자", 4), new EquipmentItem(" 의자 ", 2)))
                .isEquipmentNamesUnique()).isFalse();
        assertThat(withEquipment(List.of(new EquipmentItem("HDMI", 1), new EquipmentItem("hdmi", 1)))
                .isEquipmentNamesUnique()).isFalse();
        assertThat(withEquipment(List.of(new EquipmentItem("의자", 4), new EquipmentItem("테이블", 2)))
                .isEquipmentNamesUnique()).isTrue();
        assertThat(withEquipment(null).isEquipmentNamesUnique()).isTrue();
    }

    @Test
    @DisplayName("새 항목을 보내지 않으면 수용 인원 없음 · 비품 없음 · 가격 협의 가능으로 본다")
    void defaults() {
        SpaceDetails details = withEquipment(null).toDetails();

        assertThat(details.maxCapacity()).isNull();
        assertThat(details.equipment()).isEmpty();
        assertThat(details.priceNegotiable()).isTrue();
    }

    @Test
    @DisplayName("보낸 비품은 순서대로 엔티티 값이 된다")
    void equipmentOrder() {
        SpaceDetails details = SpaceFixtures.request(Set.of()).toDetails();

        assertThat(details.equipment()).containsExactly(new Equipment("접이식 테이블", 4), new Equipment("의자", 8));
        assertThat(details.maxCapacity()).isEqualTo(10);
    }

    private static SpaceRequest withEquipment(List<EquipmentItem> equipment) {
        SpaceRequest base = SpaceFixtures.request(Set.of(LocalDate.of(2026, 10, 21)));
        return new SpaceRequest(base.title(), base.description(), base.rules(), base.address(), base.buildingName(),
                base.unitNumber(), base.latitude(), base.longitude(), base.areaSqm(), base.buildingUse(),
                base.commercialTheme(), base.dailyPrice(), base.facilities(), base.activityTypes(),
                base.availableDates(), null, equipment, null);
    }
}
