package com.dasisiheung.controller;

import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.faq.FaqResponse;
import com.dasisiheung.entity.faq.FaqCategory;
import com.dasisiheung.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Faq", description = "자주 묻는 질문")
@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @Operation(summary = "자주 묻는 질문 목록",
            description = "로그인 없이 볼 수 있습니다. 관리자가 정한 순서대로 전체를 줍니다. category 로 거를 수 있습니다.")
    @GetMapping
    public ApiResult<List<FaqResponse>> getFaqs(@RequestParam(required = false) FaqCategory category) {
        return ApiResult.ok(faqService.getFaqs(category));
    }
}
