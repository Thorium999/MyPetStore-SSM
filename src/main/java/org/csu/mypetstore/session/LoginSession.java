package org.csu.mypetstore.session;

import jakarta.servlet.http.HttpSession;

/**
 * Session 里登录身份的读写入口。
 * KEY 用 "loginUser"，模板里可以直接写 ${session.loginUser}。
 */
public final class LoginSession {

    public static final String KEY = "loginUser";

    private LoginSession() {
    }

    public static void set(HttpSession session, LoginUser user) {
        session.setAttribute(KEY, user);
    }

    public static LoginUser get(HttpSession session) {
        return (LoginUser) session.getAttribute(KEY);
    }

    public static boolean isLoggedIn(HttpSession session) {
        return get(session) != null;
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(KEY);
    }
}
