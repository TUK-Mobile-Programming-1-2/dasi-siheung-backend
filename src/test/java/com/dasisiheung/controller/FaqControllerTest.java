package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.faq.FaqResponse;
import com.dasisiheung.entity.faq.FaqCategory;
import com.dasisiheung.service.FaqService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(FaqController.class)
class FaqControllerTest extends ApiDocsTestSupport {

    static final String TAG = "Faq";
    static final String LIST_DESCRIPTION = "로그인 없이 볼 수 있습니다. 관리자가 정한 순서대로 전체를 한 번에 줍니다(페이지 없음). "
            + "category 로 거를 수 있고, 분류의 화면 이름은 선택지 faqCategories 에 있습니다. 답변에는 줄바꿈(\\n)이 들어갈 수 있습니다.";

    @MockitoBean
    private FaqService faqService;

    @Test
    @DisplayName("자주 묻는 질문 목록: 토큰 없이 순서대로")
    void getFaqs() throws Exception {
        given(faqService.getFaqs(null)).willReturn(faqs());

        mockMvc.perform(get("/api/v1/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].category").value("APPLICATION"))
                .andDo(document("faq-list",
                        resource(TAG, "자주 묻는 질문 목록", LIST_DESCRIPTION),
                        queryParameters(
                                parameterWithName("category").optional().description("분류 (선택지 faqCategories). 없으면 전체")),
                        responseFields(success(faqFields("data[]."))).and(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("자주 묻는 질문 (보여줄 순서)"))));
    }

    @Test
    @DisplayName("자주 묻는 질문 목록: 분류로 거르기")
    void getFaqsByCategory() throws Exception {
        given(faqService.getFaqs(FaqCategory.ACCOUNT)).willReturn(List.of(faqs().get(1)));

        mockMvc.perform(get("/api/v1/faqs").param("category", "ACCOUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].category").value("ACCOUNT"));
    }

    @Test
    @DisplayName("자주 묻는 질문 목록: 없는 분류면 400 INVALID_INPUT")
    void getFaqsWithUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/faqs").param("category", "PAYMENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    static List<FaqResponse> faqs() {
        return List.of(
                new FaqResponse(3L, FaqCategory.APPLICATION, "결제는 어떻게 하나요?",
                        "앱에서는 결제하지 않습니다.\n예약이 확정되면 공간 제공자와 1:1 대화로 결제 방법을 정해 주세요."),
                new FaqResponse(1L, FaqCategory.ACCOUNT, "이용자와 공간 제공자 역할을 바꿀 수 있나요?",
                        "역할은 가입할 때 하나만 고르며 바꿀 수 없습니다."));
    }

    /** @param prefix "data." (한 건) 또는 "data[]." (목록) */
    static FieldDescriptor[] faqFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("자주 묻는 질문 ID"),
                fieldWithPath(prefix + "category").type(JsonFieldType.STRING)
                        .description("분류: ACCOUNT | APPLICATION | SPACE | COMMUNITY | ETC (화면 이름은 선택지 faqCategories)"),
                fieldWithPath(prefix + "question").type(JsonFieldType.STRING).description("질문"),
                fieldWithPath(prefix + "answer").type(JsonFieldType.STRING).description("답변 (줄바꿈 포함 가능)")
        };
    }
}
