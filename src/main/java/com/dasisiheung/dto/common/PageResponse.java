package com.dasisiheung.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 목록 응답 공통 형식. 페이지 번호는 0부터 시작한다.
 * <pre>
 * { "content": [...], "page": 0, "size": 20, "totalElements": 35, "totalPages": 2, "hasNext": true }
 * </pre>
 */
public record PageResponse<T>(
        @Schema(description = "현재 페이지 항목") List<T> content,
        @Schema(description = "현재 페이지 (0부터)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "35") long totalElements,
        @Schema(description = "전체 페이지 수", example = "2") int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return from(page.map(mapper));
    }
}
