package org.csu.mypetstore.service;

import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.session.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 校验登录。
     * 顺序刻意是「先查人 → 再比密码 → 最后看状态」：
     * 密码不正确时一律返回 BAD_CREDENTIALS，不会因为账号被禁用就泄露"这个用户名存在"。
     */
    public LoginResult authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isEmpty()) {
            return LoginResult.badCredentials();
        }

        Optional<User> found = userService.findByUsername(username.trim());
        if (found.isEmpty()) {
            return LoginResult.badCredentials();
        }

        User user = found.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return LoginResult.badCredentials();
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            return LoginResult.disabled();
        }
        return LoginResult.ok(LoginUser.from(user));
    }
}
