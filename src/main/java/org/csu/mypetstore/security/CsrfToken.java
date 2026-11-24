package org.csu.mypetstore.security;

import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 会话级 CSRF 令牌：每个 Session 一个，随表单以 _csrf 隐藏域提交，服务端比对。
 * 没有引入 Spring Security，所以这一小块自己实现。
 */
public final class CsrfToken {

    /** 放进 Session 的属性名 */
    public static final String SESSION_KEY = "csrfToken";

    /** 表单里的字段名 */
    public static final String PARAM_NAME = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTE_LENGTH = 32;

    private CsrfToken() {
    }

    public static String generate() {
        byte[] bytes = new byte[BYTE_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 取当前 Session 的令牌，没有就生成一个。 */
    public static String current(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String token && !token.isBlank()) {
            return token;
        }
        String token = generate();
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /** 常量时间比较，避免按位比较泄漏令牌前缀。 */
    public static boolean matches(HttpSession session, String submitted) {
        if (session == null || submitted == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_KEY);
        if (!(expected instanceof String token) || token.isBlank() || submitted.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
