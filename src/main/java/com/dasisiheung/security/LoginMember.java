package com.dasisiheung.security;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 컨트롤러 파라미터에 로그인 회원({@link AuthMember})을 주입한다. 비로그인 요청이면 null 이다.
 * <pre>
 * public ApiResult&lt;...&gt; me(@LoginMember AuthMember loginMember)
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
@Parameter(hidden = true)
public @interface LoginMember {
}
