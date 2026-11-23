package org.csu.mypetstore.session;

import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * 未登录时想加购的意图。拦截器把它存进 Session，登录成功后由 AccountController 补执行。
 * 这样用户不用"登录完再点一次加入购物车"。
 */
public record PendingCartAdd(Long productId, int quantity) implements Serializable {

    private static final String KEY = "pendingCartAdd";

    public static void put(HttpSession session, Long productId, int quantity) {
        session.setAttribute(KEY, new PendingCartAdd(productId, quantity));
    }

    /** 取出并清除，保证只执行一次。 */
    public static PendingCartAdd take(HttpSession session) {
        Object value = session.getAttribute(KEY);
        session.removeAttribute(KEY);
        return value instanceof PendingCartAdd pending ? pending : null;
    }
}
