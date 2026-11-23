package org.csu.mypetstore.session;

import org.csu.mypetstore.entity.User;

import java.io.Serializable;

/**
 * 放进 Session 的登录身份。只保留展示需要的字段，绝不带 password_hash。
 */
public record LoginUser(Long id, String username, String displayName, String role) implements Serializable {

    public static final String ROLE_ADMIN = "ADMIN";

    public static LoginUser from(User user) {
        return new LoginUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
