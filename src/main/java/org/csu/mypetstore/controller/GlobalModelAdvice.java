package org.csu.mypetstore.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 给所有页面注入"当前地址"（路径 + 查询串）。
 * 顶栏的「登录」链接靠它记住你从哪来，登录后能回到原页面。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
