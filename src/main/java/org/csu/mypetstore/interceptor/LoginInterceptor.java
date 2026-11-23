package org.csu.mypetstore.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.session.LoginSession;
import org.csu.mypetstore.session.PendingCartAdd;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 需要登录才能访问的路径统一在这里拦。
 * 未登录时把"想干什么"记在 Session 里（POST /cart 的加购意图），再带去登录页。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final String SIGNON = "/signon?returnUrl=";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession();
        if (LoginSession.isLoggedIn(session)) {
            return true;
        }

        String target = rememberIntentAndPickTarget(request, session);
        String encoded = URLEncoder.encode(target, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + SIGNON + encoded);
        return false;
    }

    /**
     * 加购是 POST，登录后没法重放请求体，所以把商品和数量记进 Session；
     * 登录后的落点也用商品页，用户至少能看到自己刚才在看什么。
     */
    private String rememberIntentAndPickTarget(HttpServletRequest request, HttpSession session) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/cart".equals(request.getRequestURI())) {
            Long productId = parseLong(request.getParameter("productId"));
            if (productId != null) {
                Integer quantity = parseInt(request.getParameter("quantity"));
                PendingCartAdd.put(session, productId, quantity == null ? 1 : quantity);
                return "/product?id=" + productId;
            }
        }

        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private Long parseLong(String raw) {
        try {
            return raw == null ? null : Long.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseInt(String raw) {
        try {
            return raw == null ? null : Integer.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
