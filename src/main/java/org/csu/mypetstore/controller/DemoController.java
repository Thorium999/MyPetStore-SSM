package org.csu.mypetstore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 环境自检用的临时控制器：确认 Spring MVC 已装配、请求能打到 Controller。
 * 进入业务开发前删除。
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping
    public String ping() {
        return "Spring MVC 初始化成功";
    }
}
