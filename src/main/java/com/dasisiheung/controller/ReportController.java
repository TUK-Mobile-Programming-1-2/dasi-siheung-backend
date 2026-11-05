package com.dasisiheung.controller;

import com.dasisiheung.config.OpenApiConfig;
import com.dasisiheung.dto.common.ApiResult;
import com.dasisiheung.dto.common.PageQuery;
import com.dasisiheung.dto.common.PageResponse;
import com.dasisiheung.dto.report.MyReportResponse;
import com.dasisiheung.dto.report.ReportRequest;
import com.dasisiheung.dto.report.ReportResponse;
import com.dasisiheung.entity.report.ReportStatus;
import com.dasisiheung.security.AuthMember;
import com.dasisiheung.security.LoginMember;
import com.dasisiheung.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "신고")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고하기",
            description = "공간 · 게시글 · 댓글 · 회원을 신고합니다. 관리자가 확인 후 숨김 · 이용 정지 · 기각합니다. "
                    + "multipart 파트: targetType · targetId · reason · detail(선택) 텍스트, "
                    + "photos(증거 사진, 선택, 최대 5장, JPG·PNG·WEBP 10MB). 사진은 관리자만 볼 수 있습니다.")
    @PostMapping(path = "/api/v1/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<ReportResponse> report(@LoginMember AuthMember loginMember,
                                            @Valid @ModelAttribute ReportRequest request) {
        return ApiResult.ok(reportService.report(loginMember.id(), request));
    }

    @Operation(summary = "내 신고 내역",
            description = "내가 한 신고, 최근 순. 처리 결과는 상태(PENDING 처리 대기 · RESOLVED 조치 완료 · DISMISSED 기각)만 "
                    + "알려주며, 관리자 메모 · 처리 내용 · 증거 사진 주소는 주지 않습니다.")
    @GetMapping("/api/v1/members/me/reports")
    public ApiResult<PageResponse<MyReportResponse>> getMyReports(@LoginMember AuthMember loginMember,
                                                                  @RequestParam(required = false) ReportStatus status,
                                                                  @Valid @ParameterObject PageQuery pageQuery) {
        return ApiResult.ok(reportService.getMyReports(loginMember.id(), status, pageQuery));
    }
}
