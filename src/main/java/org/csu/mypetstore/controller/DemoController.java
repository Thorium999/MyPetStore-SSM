package org.csu.mypetstore.controller;

import org.csu.mypetstore.dto.UserView;
import org.csu.mypetstore.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 环境自检用的临时控制器：确认 MVC、ORM 与分层链路都通。
 * 进入业务开发前删除。
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final UserService userService;

    public DemoController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String ping() {
        return "Spring MVC 初始化成功";
    }

    @GetMapping("/user")
    public UserView user(@RequestParam String username) {
        return userService.findByUsername(username)
                .map(UserView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
}
