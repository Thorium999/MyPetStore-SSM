package org.csu.mypetstore.dto;

import org.csu.mypetstore.entity.User;

/**
 * 对外输出的用户视图：不含 password_hash。进入业务后可扩展。
 */
public record UserView(Long id, String username, String displayName, String role, String status) {

public static UserView of(User user) {
    return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getStatus());
}

}
