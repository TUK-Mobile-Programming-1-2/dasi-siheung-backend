package com.dasisiheung.controller;

import static com.dasisiheung.support.ApiDocumentation.document;
import static com.dasisiheung.support.ApiDocumentation.error;
import static com.dasisiheung.support.ApiDocumentation.resource;
import static com.dasisiheung.support.ApiDocumentation.success;
import static com.dasisiheung.support.ApiDocumentation.validationError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dasisiheung.dto.auth.LoginRequest;
import com.dasisiheung.dto.auth.RefreshTokenRequest;
import com.dasisiheung.dto.auth.SignupRequest;
import com.dasisiheung.dto.auth.TokenResponse;
import com.dasisiheung.dto.member.MemberSummaryResponse;
import com.dasisiheung.entity.member.Role;
import com.dasisiheung.exception.BusinessException;
import com.dasisiheung.exception.ErrorCode;
import com.dasisiheung.service.AuthService;
import com.dasisiheung.support.ApiDocsTest;
import com.dasisiheung.support.ApiDocsTestSupport;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApiDocsTest(AuthController.class)
class AuthControllerTest extends ApiDocsTestSupport {

    private static final String TAG = "Auth";
    private static final String REFRESH_TOKEN = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4tZXhhbXBsZQ";
    private static final MemberSummaryResponse MEMBER =
            new MemberSummaryResponse(USER_ID, "user@example.com", "김시흥", Role.USER);

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("회원가입: 201 과 가입한 회원 정보를 반환한다")
    void signup() throws Exception {
        given(authService.signup(any(SignupRequest.class))).willReturn(MEMBER);
        SignupRequest request = new SignupRequest("user@example.com", "password123", "김시흥", "010-1234-5678", Role.USER);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andDo(document("auth-signup",
                        resource(TAG, "회원가입", "이용자(USER) 또는 공간 제공자(PROVIDER)로 가입합니다. "
                                + "ADMIN 은 400 ROLE_NOT_ALLOWED, 이미 가입된 이메일은 409 DUPLICATE_EMAIL 입니다."),
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 (로그인 아이디, 최대 254자)"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (영문·숫자 포함 8~64자)"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("이름 (최대 30자)"),
                                fieldWithPath("phone").type(JsonFieldType.STRING).description("휴대폰 번호 (하이픈 선택)"),
                                fieldWithPath("role").type(JsonFieldType.STRING).description("역할: USER(이용자) | PROVIDER(공간 제공자)")),
                        responseFields(success(memberFields("data.")))));
    }

    @Test
    @DisplayName("회원가입: 입력값이 형식에 맞지 않으면 400 과 필드별 사유를 반환한다")
    void signupWithInvalidInput() throws Exception {
        SignupRequest request = new SignupRequest("not-email", "short", "", "02-123-4567", null);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors.length()").value(5))
                .andDo(document("auth-signup-invalid",
                        resource(TAG, "회원가입", "입력값 검증 실패 예시"),
                        responseFields(validationError())));
    }

    @Test
    @DisplayName("회원가입: 이미 가입된 이메일이면 409 DUPLICATE_EMAIL")
    void signupWithDuplicateEmail() throws Exception {
        given(authService.signup(any(SignupRequest.class))).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));
        SignupRequest request = new SignupRequest("user@example.com", "password123", "김시흥", "01012345678", Role.USER);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"))
                .andDo(document("auth-signup-duplicate",
                        resource(TAG, "회원가입", "이미 가입된 이메일 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("로그인: 액세스·리프레시 토큰과 회원 정보를 반환한다")
    void login() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("user@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
                .andDo(document("auth-login",
                        resource(TAG, "로그인", "이메일·비밀번호가 틀리면 401 INVALID_CREDENTIALS, "
                                + "이용 정지 회원은 403 ACCOUNT_SUSPENDED 입니다."),
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")),
                        responseFields(success(tokenFields()))));
    }

    @Test
    @DisplayName("로그인: 이메일 또는 비밀번호가 틀리면 401 INVALID_CREDENTIALS")
    void loginWithWrongPassword() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("user@example.com", "wrong-password1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andDo(document("auth-login-fail",
                        resource(TAG, "로그인", "로그인 실패 예시"),
                        responseFields(error())));
    }

    @Test
    @DisplayName("토큰 재발급: 새 토큰 쌍을 반환한다")
    void refresh() throws Exception {
        given(authService.refresh(anyString())).willReturn(tokenResponse());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshTokenRequest(REFRESH_TOKEN))))
                .andExpect(status().isOk())
                .andDo(document("auth-refresh",
                        resource(TAG, "토큰 재발급", "사용한 리프레시 토큰은 폐기됩니다. 폐기·만료된 토큰이면 401 INVALID_REFRESH_TOKEN 이며, "
                                + "이때는 다시 로그인해야 합니다."),
                        requestFields(
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰")),
                        responseFields(success(tokenFields()))));
        then(authService).should().refresh(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃: 리프레시 토큰을 폐기하고 성공만 반환한다")
    void logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshTokenRequest(REFRESH_TOKEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("auth-logout",
                        resource(TAG, "로그아웃", "이미 폐기된 토큰이어도 성공으로 응답합니다."),
                        requestFields(
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰")),
                        responseFields(success())));
        then(authService).should().logout(REFRESH_TOKEN);
    }

    private TokenResponse tokenResponse() {
        return TokenResponse.of(userToken().substring("Bearer ".length()), Duration.ofMinutes(30),
                REFRESH_TOKEN, Duration.ofDays(14), MEMBER);
    }

    private static FieldDescriptor[] tokenFields() {
        FieldDescriptor[] tokenFields = {
                fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 종류 (Bearer)"),
                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰 (JWT). Authorization: Bearer {accessToken}"),
                fieldWithPath("data.accessTokenExpiresIn").type(JsonFieldType.NUMBER).description("액세스 토큰 유효 시간(초)"),
                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰 (재발급 때마다 바뀜)"),
                fieldWithPath("data.refreshTokenExpiresIn").type(JsonFieldType.NUMBER).description("리프레시 토큰 유효 시간(초)")
        };
        FieldDescriptor[] memberFields = memberFields("data.member.");
        FieldDescriptor[] all = new FieldDescriptor[tokenFields.length + memberFields.length];
        System.arraycopy(tokenFields, 0, all, 0, tokenFields.length);
        System.arraycopy(memberFields, 0, all, tokenFields.length, memberFields.length);
        return all;
    }

    static FieldDescriptor[] memberFields(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath(prefix + "email").type(JsonFieldType.STRING).description("이메일"),
                fieldWithPath(prefix + "name").type(JsonFieldType.STRING).description("이름"),
                fieldWithPath(prefix + "role").type(JsonFieldType.STRING).description("역할: USER | PROVIDER | ADMIN")
        };
    }
}
