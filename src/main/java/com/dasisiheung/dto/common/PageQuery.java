package com.dasisiheung.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 목록 조회의 page · size 쿼리 파라미터. 컨트롤러에서 {@code @Valid @ParameterObject PageQuery} 로 받는다.
 */
public record PageQuery(
        @Schema(description = "페이지 번호 (0부터)", example = "0", defaultValue = "0")
        @Min(0) Integer page,

        @Schema(description = "페이지 크기 (1~100)", example = "20", defaultValue = "20")
        @Min(1) @Max(MAX_SIZE) Integer size
) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = page == null ? 0 : page;
        size = size == null ? DEFAULT_SIZE : size;
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
