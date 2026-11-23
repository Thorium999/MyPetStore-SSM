package org.csu.mypetstore.controller;

import jakarta.servlet.http.HttpSession;
import org.csu.mypetstore.service.AccountService;
import org.csu.mypetstore.service.LoginResult;
import org.csu.mypetstore.session.LoginSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 登录 / 退出。只做转发与提示文案，校验逻辑全在 AccountService。
 */
@Controller
public class AccountController {

private static final String LOGIN_VIEW = "signon";
private static final String HOME = "/main";

private final AccountService accountService;

public AccountController(AccountService accountService) {
    this.accountService = accountService;
}

@GetMapping("/signon")
public String signonPage(@RequestParam(required = false) String returnUrl,
                         HttpSession session,
                         Model model) {
    if (LoginSession.isLoggedIn(session))
        return "redirect:" + HOME;

    // 把"从哪来"带进表单，登录成功后才能真正回到原页面
    model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl));
    return LOGIN_VIEW;
}

@PostMapping("/signon")
public String signon(@RequestParam String username,
                     @RequestParam String password,
                     @RequestParam(required = false) String returnUrl,
                     HttpSession session,
                     Model model) {
    LoginResult result = accountService.authenticate(username, password);
    if (result.isOk()) {
        LoginSession.set(session, result.user());
        // 回跳地址只可能是站内路径：safeReturnUrl 把绝对地址、//host、/\host 全部替换成 /main
        // （Security 检查会把这个变量当"用户可控"，属于误报；三种攻击写法见 AccountControllerTest.refusesOffsiteReturnUrl）
        String target = safeReturnUrl(returnUrl);
        return "redirect:" + target;
    }
    
    model.addAttribute("error", (result.status() == LoginResult.Status.DISABLED) ?
                                "该账号已被禁用，请联系管理员" : "用户名或密码错误");
    model.addAttribute("username", username);
    model.addAttribute("returnUrl", returnUrl);
    return LOGIN_VIEW;
}

@GetMapping("/signout")
public String signout(HttpSession session) {
    LoginSession.clear(session);
    return "redirect:" + HOME;
}

/**
 * 回跳目标：安全就用它，否则退回首页。
 * 挡三类写法：http(s):// 绝对地址、//host 协议相对地址、/\host（浏览器会把反斜杠当斜杠）。
 */
private String safeReturnUrl(String returnUrl) {
    return isSafeInternalPath(returnUrl) ? returnUrl.trim() : HOME;
}

/** 登录页回填用：不安全的直接丢掉，别让它进隐藏域。登录页自身也不做回跳目标。 */
private String normalizeReturnUrl(String returnUrl) {
    if (!isSafeInternalPath(returnUrl)) {
        return null;
    }
    String url = returnUrl.trim();
    return url.startsWith("/signon") ? null : url;
}

private boolean isSafeInternalPath(String url) {
    if (url == null) {
        return false;
    }
    String value = url.trim();
    return value.startsWith("/") && !value.startsWith("//") && !value.contains("\\");
}

}
