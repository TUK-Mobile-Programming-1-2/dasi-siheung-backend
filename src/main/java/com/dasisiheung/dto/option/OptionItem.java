package com.dasisiheung.dto.option;

import com.dasisiheung.entity.LabeledEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public record OptionItem(
        @Schema(description = "요청·응답에 쓰는 코드", example = "POPUP_STORE") String code,
        @Schema(description = "화면에 보여줄 이름", example = "팝업 판매") String label
) {

    public static OptionItem from(LabeledEnum value) {
        return new OptionItem(value.name(), value.getLabel());
    }

    public static <E extends Enum<E> & LabeledEnum> List<OptionItem> listOf(Class<E> enumType) {
        return listOf(enumType, value -> true);
    }

    public static <E extends Enum<E> & LabeledEnum> List<OptionItem> listOf(Class<E> enumType, Predicate<E> filter) {
        return Arrays.stream(enumType.getEnumConstants()).filter(filter).map(OptionItem::from).toList();
    }
}
