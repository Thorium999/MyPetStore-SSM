package org.csu.mypetstore.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.csu.mypetstore.security.CsrfToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * 校验所有写操作的 CSRF 令牌（POST / PUT / PATCH / DELETE）。
 * GET 是安全方法，不校验——否则首页、列表这些只读请求也得带令牌。
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CsrfInterceptor.class);

    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!UNSAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }

        String submitted = request.getParameter(CsrfToken.PARAM_NAME);
        if (CsrfToken.matches(request.getSession(false), submitted)) {
            return true;
        }

        log.warn("拒绝缺少或错误 CSRF 令牌的请求：{} {}", request.getMethod(), request.getRequestURI());
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "表单已过期，请刷新页面后重试");
        return false;
    }
}
