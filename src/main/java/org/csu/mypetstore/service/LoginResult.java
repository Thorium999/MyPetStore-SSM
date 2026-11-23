package org.csu.mypetstore.service;

import org.csu.mypetstore.session.LoginUser;

/**
 * 登录结果。区分"密码错"和"账号禁用"，但页面上的文案由 Controller 决定：
 * 密码错一律说"用户名或密码错误"，避免暴露某个用户名是否存在。
 */
public record LoginResult(Status status, LoginUser user) {

public enum Status {
    OK,
    BAD_CREDENTIALS,
    DISABLED
}

public static LoginResult ok(LoginUser user) {
    return new LoginResult(Status.OK, user);
}

public static LoginResult badCredentials() {
    return new LoginResult(Status.BAD_CREDENTIALS, null);
}

public static LoginResult disabled() {
    return new LoginResult(Status.DISABLED, null);
}

public boolean isOk() {
    return status == Status.OK;
}

}
