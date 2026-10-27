package com.dasisiheung.support;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Snippet;

/**
 * REST Docs 문서화 보일러플레이트를 줄이는 도우미.
 * <pre>
 * .andDo(document("auth-login", resource("Auth", "로그인", "설명"),
 *         requestFields(...), responseFields(success(...))))
 * </pre>
 * 표준 스니펫(requestFields 등)이 실제 요청·응답과 일치하는지 검증되고, 같은 정보로 OpenAPI 명세가 만들어진다.
 */
public final class ApiDocumentation {

    private ApiDocumentation() {
    }

    public static RestDocumentationResultHandler document(String identifier, ResourceSnippetParametersBuilder resource,
                                                          Snippet... snippets) {
        return MockMvcRestDocumentationWrapper.document(identifier, resource, snippets);
    }

    public static ResourceSnippetParametersBuilder resource(String tag, String summary, String description) {
        return ResourceSnippetParameters.builder().tag(tag).summary(summary).description(description);
    }

    public static HeaderDescriptor authorization() {
        return headerWithName(HttpHeaders.AUTHORIZATION).description("Bearer {액세스 토큰}");
    }

    /** 성공 응답: success + 데이터 필드(경로는 "data." 로 시작해야 한다). */
    public static List<FieldDescriptor> success(FieldDescriptor... dataFields) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"));
        fields.addAll(Arrays.asList(dataFields));
        return fields;
    }

    /** 실패 응답: success + error.code / error.message. */
    public static List<FieldDescriptor> error() {
        return List.of(
                fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (false)"),
                fieldWithPath("error.code").type(JsonFieldType.STRING).description("오류 코드"),
                fieldWithPath("error.message").type(JsonFieldType.STRING).description("오류 메시지"));
    }

    /** 실패 응답 + 입력값 검증 실패 목록. */
    public static List<FieldDescriptor> validationError() {
        List<FieldDescriptor> fields = new ArrayList<>(error());
        fields.add(fieldWithPath("error.fieldErrors[].field").type(JsonFieldType.STRING).description("검증에 실패한 필드"));
        fields.add(fieldWithPath("error.fieldErrors[].reason").type(JsonFieldType.STRING).description("실패 사유"));
        return fields;
    }

    /**
     * 페이지 응답 공통 필드. 목록 항목 필드는 "data.content[]." 로 시작해야 한다.
     */
    public static List<FieldDescriptor> page(FieldDescriptor... contentFields) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("요청 성공 여부 (true)"));
        fields.add(fieldWithPath("data.content").type(JsonFieldType.ARRAY).description("현재 페이지 항목"));
        fields.addAll(Arrays.asList(contentFields));
        fields.add(fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지 (0부터)"));
        fields.add(fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지 크기"));
        fields.add(fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"));
        fields.add(fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"));
        fields.add(fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"));
        return fields;
    }
}
