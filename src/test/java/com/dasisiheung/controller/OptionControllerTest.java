package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.service.OptionService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(OptionController.class)
class OptionControllerTest extends ApiDocsTestSupport {

    /** 응답의 선택지 그룹 이름과 설명. 그룹을 추가하면 여기에도 추가한다. */
    static final String[][] GROUPS = {
            {"signupRoles", "가입할 수 있는 역할 (USER, PROVIDER)"},
            {"regions", "서비스 지역"},
            {"activityTypes", "허용 활동 · 신청 활동 종류"},
            {"facilities", "보유 시설"},
            {"buildingUses", "건축물 용도"},
            {"commercialThemes", "상권 테마"},
            {"usePurposes", "이용 목적"}
    };

    @MockitoBean
    private OptionService optionService;

    @Test
    @DisplayName("선택지 조회: 인증 없이 그룹별 code·label 목록을 반환하고 1시간 캐시할 수 있다")
    void getOptions() throws Exception {
        given(optionService.getOptions()).willReturn(new OptionService().getOptions());

        mockMvc.perform(get("/api/v1/options"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=3600, public"))
                .andExpect(jsonPath("$.data.signupRoles.length()").value(2))
                .andExpect(jsonPath("$.data.regions[0].code").value("JEONGWANG"))
                .andExpect(jsonPath("$.data.regions[0].label").value("정왕동"))
                .andDo(document("option-list",
                        resource("Option", "선택지 조회", "화면의 선택 목록입니다. 요청에는 code 를, 화면에는 label 을 쓰세요. "
                                + "앱 시작 시 한 번 받아 두면 됩니다."),
                        responseFields(success(groupFields()))));
    }

    private static FieldDescriptor[] groupFields() {
        List<FieldDescriptor> fields = new ArrayList<>();
        for (String[] group : GROUPS) {
            fields.add(fieldWithPath("data." + group[0]).type(JsonFieldType.ARRAY).description(group[1]));
            fields.add(fieldWithPath("data." + group[0] + "[].code").type(JsonFieldType.STRING).description("코드"));
            fields.add(fieldWithPath("data." + group[0] + "[].label").type(JsonFieldType.STRING).description("화면 이름"));
        }
        return fields.toArray(FieldDescriptor[]::new);
    }
}
