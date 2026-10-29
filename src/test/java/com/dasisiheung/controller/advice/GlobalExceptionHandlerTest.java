package com.dasisiheung.controller.advice;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("겹치는 확정 예약(DB 배타 제약 23P01)은 409 PERIOD_ALREADY_BOOKED")
    void exclusionViolation() throws Exception {
        mockMvc.perform(get("/stub/double-booking"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_ALREADY_BOOKED"));
    }

    @Test
    @DisplayName("요청 본문 검증에 실패하면 400 과 필드별 오류 목록을 반환한다")
    void invalidRequestBody() throws Exception {
        mockMvc.perform(post("/stub/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "price": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.error.fieldErrors[*].field", containsInAnyOrder("name", "price")));
    }

    @Test
    @DisplayName("JSON 형식이 깨진 요청은 400 을 반환한다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/stub/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400 을 반환한다")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/stub/spaces/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("BusinessException 은 ErrorCode 의 상태 코드와 상세 메시지로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(get("/stub/spaces/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405 를 반환한다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(delete("/stub/spaces/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("JSON 이 아닌 본문은 415 를 반환한다")
    void unsupportedMediaType() throws Exception {
        mockMvc.perform(post("/stub/spaces")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=popup"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 을 반환하고 내부 메시지를 노출하지 않는다")
    void unexpectedException() throws Exception {
        mockMvc.perform(get("/stub/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(content().string(not(containsString(StubController.INTERNAL_DETAIL))));
    }

    @Test
    @DisplayName("DB 제약 위반은 409 DATA_CONFLICT 로 응답하고 SQL 메시지를 노출하지 않는다")
    void dataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/stub/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DATA_CONFLICT"))
                .andExpect(content().string(not(containsString(StubController.INTERNAL_DETAIL))));
    }

    @Test
    @DisplayName("인가 예외는 직접 응답하지 않고 Spring Security 로 넘긴다")
    void accessDeniedIsDelegatedToSecurity() {
        assertThatThrownBy(() -> mockMvc.perform(get("/stub/admin-only")))
                .hasCauseInstanceOf(AccessDeniedException.class);
    }

    @RestController
    @RequestMapping("/stub")
    static class StubController {

        static final String INTERNAL_DETAIL = "jdbc-connection-secret";

        @PostMapping("/spaces")
        ApiResult<Void> create(@Valid @RequestBody StubRequest request) {
            return ApiResult.ok();
        }

        @GetMapping("/spaces/{id}")
        ApiResult<Void> find(@PathVariable Long id) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "공간을 찾을 수 없습니다.");
        }

        @GetMapping("/crash")
        ApiResult<Void> crash() {
            throw new IllegalStateException(INTERNAL_DETAIL);
        }

        @GetMapping("/conflict")
        ApiResult<Void> conflict() {
            throw new DataIntegrityViolationException(INTERNAL_DETAIL);
        }

        @GetMapping("/double-booking")
        ApiResult<Void> doubleBooking() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new java.sql.SQLException("conflicting key value violates exclusion constraint", "23P01"));
        }

        @GetMapping("/admin-only")
        ApiResult<Void> adminOnly() {
            throw new AccessDeniedException("관리자 전용");
        }
    }

    record StubRequest(@NotBlank String name, @PositiveOrZero Integer price) {
    }
}
