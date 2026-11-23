package org.csu.mypetstore.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.service.CartService;
import org.csu.mypetstore.session.LoginSession;
import org.csu.mypetstore.session.LoginUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 给所有页面注入两个公共属性：
 * currentPath —— 当前地址（顶栏「登录」带它做回跳）
 * cartCount   —— 顶栏购物车角标；未登录恒为 0，登录后查一次库
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CartService cartService;

    public GlobalModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        LoginUser user = LoginSession.get(session);
        return user == null ? 0 : cartService.countItems(user.id());
    }
}
