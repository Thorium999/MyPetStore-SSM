package org.csu.mypetstore.controller;

import org.csu.mypetstore.entity.User;
import org.csu.mypetstore.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * 环境自检用：确认 Thymeleaf 能把 Model 渲染成 HTML。进入业务开发前删除。
 */
@Controller
public class DemoPageController {

    private final UserService userService;

    public DemoPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/demo/user-page")
    public String userPage(@RequestParam String username, Model model) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        model.addAttribute("user", user);
        return "user";
    }
}
