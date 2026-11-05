package com.dasisiheung.dto.space;

import com.dasisiheung.entity.space.Equipment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 빌려주는 비품 한 줄. 공간 등록 · 수정 요청과 상세 응답에 함께 쓴다. */
public record EquipmentItem(
        @Schema(description = "비품 이름", example = "접이식 테이블")
        @NotBlank @Size(max = Equipment.MAX_NAME_LENGTH) String name,

        @Schema(description = "수량 (1~999)", example = "4")
        @Min(1) @Max(Equipment.MAX_QUANTITY) int quantity
) {

    public static EquipmentItem from(Equipment equipment) {
        return new EquipmentItem(equipment.getName(), equipment.getQuantity());
    }

    public Equipment toEntity() {
        return new Equipment(name, quantity);
    }
}
