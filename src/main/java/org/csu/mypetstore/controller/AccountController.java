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

import java.util.List;

/**
 * 登录 / 退出。只做转发与提示文案，校验逻辑全在 AccountService。
 */
@Controller
public class AccountController {

private static final String LOGIN_VIEW = "signon";
private static final String HOME = "/main";

/**
 * 允许作为登录回跳目标的页面（只列"看"的页面，不列"做"的动作）。
 * 白名单而不是黑名单：以后新增有副作用的 GET/动作不会被顺手放进来。
 * 注意 /signout 是 POST，本来也不在这里。
 */
private static final List<String> RETURN_PAGES = List.of("/main", "/category", "/product", "/cart", "/orders");

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

/**
 * 退出必须是 POST：GET 是"安全方法"，浏览器预取、链接预加载都会打到它，
 * 用 GET 退出会导致"打开页面就被登出"。这里顺带作废整个会话。
 */
@PostMapping("/signout")
public String signout(HttpSession session) {
    session.invalidate();
    return "redirect:" + HOME;
}

/**
 * 回跳目标：在白名单页面内就用它，否则退回首页。
 * 另外挡住三类写法：http(s):// 绝对地址、//host 协议相对地址、/\host（浏览器会把反斜杠当斜杠）。
 */
private String safeReturnUrl(String returnUrl) {
    return isAllowedPage(returnUrl) ? returnUrl.trim() : HOME;
}

/** 登录页回填用：不合规的直接丢掉，别让它进隐藏域。 */
private String normalizeReturnUrl(String returnUrl) {
    return isAllowedPage(returnUrl) ? returnUrl.trim() : null;
}

private boolean isAllowedPage(String url) {
    if (url == null) {
        return false;
    }
    String value = url.trim();
    if (!value.startsWith("/") || value.startsWith("//") || value.contains("\\")) {
        return false;
    }
    return RETURN_PAGES.stream().anyMatch(page -> value.equals(page)
                                                  || value.startsWith(page + "?")
                                                  || value.startsWith(page + "/"));
}

}
