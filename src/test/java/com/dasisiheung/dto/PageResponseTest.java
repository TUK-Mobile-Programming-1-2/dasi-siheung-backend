package com.dasisiheung.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    @DisplayName("Page 를 0부터 시작하는 페이지 응답으로 바꾸고 항목을 변환한다")
    void fromPage() {
        var page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 5);

        PageResponse<String> response = PageResponse.from(page, number -> "#" + number);

        assertThat(response.content()).containsExactly("#1", "#2");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("page·size 를 보내지 않으면 0페이지 20개")
    void defaultPageQuery() {
        PageQuery query = new PageQuery(null, null);

        assertThat(query.toPageable().getPageNumber()).isZero();
        assertThat(query.toPageable().getPageSize()).isEqualTo(PageQuery.DEFAULT_SIZE);
    }
}
