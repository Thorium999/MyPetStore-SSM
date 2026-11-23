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
public String signonPage(HttpSession session) {
    if (LoginSession.isLoggedIn(session))
        return "redirect:" + HOME;
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
 * 只接受站内相对路径。挡三类写法：
 * http(s):// 绝对地址、//host 协议相对地址、/\host（浏览器会把反斜杠当斜杠）。
 */
private String safeReturnUrl(String returnUrl) {
    if (returnUrl == null)
        return HOME;
    String url = returnUrl.trim();
    if (!url.startsWith("/") || url.startsWith("//") || url.contains("\\"))
        return HOME;
    return url;
}

}
