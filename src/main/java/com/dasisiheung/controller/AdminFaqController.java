package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.faq.FaqOrderRequest;
import com.dasisiheung.dto.faq.FaqRequest;
import com.dasisiheung.dto.faq.FaqResponse;
import com.dasisiheung.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리자(ADMIN) 자주 묻는 질문 관리. 목록은 공개 API(GET /api/v1/faqs)를 같이 쓴다. */
@Tag(name = "Admin Faq", description = "관리자 — 자주 묻는 질문 관리")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqController {

    private final FaqService faqService;

    @Operation(summary = "자주 묻는 질문 추가", description = "맨 뒤에 붙습니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<FaqResponse> create(@Valid @RequestBody FaqRequest request) {
        return ApiResult.ok(faqService.create(request));
    }

    @Operation(summary = "자주 묻는 질문 수정", description = "분류 · 질문 · 답변을 모두 보냅니다. 순서는 그대로입니다.")
    @PutMapping("/{faqId}")
    public ApiResult<FaqResponse> update(@PathVariable Long faqId, @Valid @RequestBody FaqRequest request) {
        return ApiResult.ok(faqService.update(faqId, request));
    }

    @Operation(summary = "자주 묻는 질문 삭제")
    @DeleteMapping("/{faqId}")
    public ApiResult<Void> delete(@PathVariable Long faqId) {
        faqService.delete(faqId);
        return ApiResult.ok();
    }

    @Operation(summary = "자주 묻는 질문 순서 변경",
            description = "모든 ID 를 보여줄 순서대로 한 번씩 보냅니다. 빠지거나 겹치거나 없는 ID 가 있으면 400 INVALID_FAQ_ORDER "
                    + "(그사이 다른 관리자가 추가 · 삭제한 경우 포함 — 목록을 다시 불러오세요). 응답은 바뀐 전체 목록입니다.")
    @PutMapping("/order")
    public ApiResult<List<FaqResponse>> reorder(@Valid @RequestBody FaqOrderRequest request) {
        return ApiResult.ok(faqService.reorder(request.faqIds()));
    }
}
