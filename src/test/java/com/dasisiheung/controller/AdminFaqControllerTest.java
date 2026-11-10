package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.authorization;
import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.ApiDocumentation.validationError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.faq.FaqRequest;
import com.dasisiheung.dto.faq.FaqResponse;
import com.dasisiheung.entity.faq.FaqCategory;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.FaqService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(AdminFaqController.class)
class AdminFaqControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Admin Faq";
    private static final String BASE = "/api/v1/admin/faqs";
    private static final String ORDER_DESCRIPTION = "모든 ID 를 보여줄 순서대로 한 번씩 보냅니다(첫 번째가 맨 위). "
            + "빠지거나 겹치거나 없는 ID 가 있으면 400 INVALID_FAQ_ORDER — 그사이 다른 관리자가 추가 · 삭제한 경우이니 목록을 다시 불러오세요. "
            + "응답은 바뀐 전체 목록입니다.";
    private static final String BODY = """
            {"category": "APPLICATION", "question": "결제는 어떻게 하나요?", "answer": "앱에서는 결제하지 않습니다."}
            """;
    private static final FaqResponse FAQ = new FaqResponse(10L, FaqCategory.APPLICATION, "결제는 어떻게 하나요?",
            "앱에서는 결제하지 않습니다.");

    @MockitoBean
    private FaqService faqService;

    @Test
    @DisplayName("추가: 201, 맨 뒤에 붙는다")
    void create() throws Exception {
        given(faqService.create(any(FaqRequest.class))).willReturn(FAQ);

        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andDo(document("admin-faq-create",
                        resource(TAG, "자주 묻는 질문 추가", "맨 뒤에 붙습니다. 순서는 '순서 변경'으로 바꿉니다."),
                        requestHeaders(authorization()),
                        requestFields(requestFieldDescriptors()),
                        responseFields(success(FaqControllerTest.faqFields("data.")))));
        then(faqService).should().create(new FaqRequest(FaqCategory.APPLICATION, "결제는 어떻게 하나요?",
                "앱에서는 결제하지 않습니다."));
    }

    @Test
    @DisplayName("추가: 질문이 비었거나 분류가 없으면 400 INVALID_INPUT")
    void createWithInvalidBody() throws Exception {
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": " ", "answer": "답변"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors.length()").value(2))
                .andDo(document("admin-faq-create-invalid",
                        resource(TAG, "자주 묻는 질문 추가", "항목이 비었거나 길이를 넘은 예시"),
                        responseFields(validationError())));
        then(faqService).should(never()).create(any());
    }

    @Test
    @DisplayName("관리자가 아니면 403 FORBIDDEN")
    void createByUser() throws Exception {
        mockMvc.perform(post(BASE)
                        .header(HttpHeaders.AUTHORIZATION, userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andDo(document("admin-faq-forbidden",
                        resource(TAG, "자주 묻는 질문 추가", "관리자가 아닌 회원이 호출한 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("수정: 순서는 그대로")
    void update() throws Exception {
        given(faqService.update(eq(10L), any(FaqRequest.class))).willReturn(FAQ);

        mockMvc.perform(put(BASE + "/{faqId}", 10L)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question").value("결제는 어떻게 하나요?"))
                .andDo(document("admin-faq-update",
                        resource(TAG, "자주 묻는 질문 수정", "분류 · 질문 · 답변을 모두 보냅니다. 순서는 그대로입니다. "
                                + "없는 ID 면 404 FAQ_NOT_FOUND."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("faqId").description("자주 묻는 질문 ID")),
                        requestFields(requestFieldDescriptors()),
                        responseFields(success(FaqControllerTest.faqFields("data.")))));
    }

    @Test
    @DisplayName("수정: 없는 질문이면 404 FAQ_NOT_FOUND")
    void updateNotFound() throws Exception {
        given(faqService.update(eq(99L), any(FaqRequest.class)))
                .willThrow(new BusinessException(ErrorCode.FAQ_NOT_FOUND));

        mockMvc.perform(put(BASE + "/{faqId}", 99L)
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FAQ_NOT_FOUND"))
                .andDo(document("admin-faq-not-found",
                        resource(TAG, "자주 묻는 질문 수정", "없는 ID 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("삭제")
    void deleteFaq() throws Exception {
        mockMvc.perform(delete(BASE + "/{faqId}", 10L).header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andDo(document("admin-faq-delete",
                        resource(TAG, "자주 묻는 질문 삭제", "없는 ID 면 404 FAQ_NOT_FOUND."),
                        requestHeaders(authorization()),
                        pathParameters(parameterWithName("faqId").description("자주 묻는 질문 ID")),
                        responseFields(success())));
        then(faqService).should().delete(10L);
    }

    @Test
    @DisplayName("순서 변경: 바뀐 전체 목록을 준다")
    void reorder() throws Exception {
        given(faqService.reorder(List.of(3L, 1L))).willReturn(FaqControllerTest.faqs());

        mockMvc.perform(put(BASE + "/order")
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"faqIds": [3, 1]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andDo(document("admin-faq-reorder",
                        resource(TAG, "자주 묻는 질문 순서 변경", ORDER_DESCRIPTION),
                        requestHeaders(authorization()),
                        requestFields(fieldWithPath("faqIds").type(JsonFieldType.ARRAY)
                                .description("모든 자주 묻는 질문 ID (보여줄 순서대로)")),
                        responseFields(success(FaqControllerTest.faqFields("data[]."))).and(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("바뀐 순서의 전체 목록"))));
    }

    @Test
    @DisplayName("순서 변경: 목록이 어긋나면 400 INVALID_FAQ_ORDER")
    void reorderMismatch() throws Exception {
        willThrow(new BusinessException(ErrorCode.INVALID_FAQ_ORDER)).given(faqService).reorder(anyList());

        mockMvc.perform(put(BASE + "/order")
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"faqIds": [3]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FAQ_ORDER"))
                .andDo(document("admin-faq-reorder-invalid",
                        resource(TAG, "자주 묻는 질문 순서 변경", ORDER_DESCRIPTION),
                        responseFields(error())));
    }

    @Test
    @DisplayName("순서 변경: 빈 목록이면 400 INVALID_INPUT")
    void reorderEmpty() throws Exception {
        mockMvc.perform(put(BASE + "/order")
                        .header(HttpHeaders.AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"faqIds": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        then(faqService).should(never()).reorder(anyList());
    }

    private static FieldDescriptor[] requestFieldDescriptors() {
        return new FieldDescriptor[]{
                fieldWithPath("category").type(JsonFieldType.STRING)
                        .description("분류: ACCOUNT | APPLICATION | SPACE | COMMUNITY | ETC (선택지 faqCategories)"),
                fieldWithPath("question").type(JsonFieldType.STRING).description("질문 (최대 200자)"),
                fieldWithPath("answer").type(JsonFieldType.STRING).description("답변 (최대 2000자, 줄바꿈 가능)")
        };
    }
}
