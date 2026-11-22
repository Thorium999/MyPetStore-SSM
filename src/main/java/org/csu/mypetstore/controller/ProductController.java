package org.csu.mypetstore.controller;

import org.csu.mypetstore.service.CategoryService;
import org.csu.mypetstore.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 商品相关页面控制器。每个方法只做三件事：调 Service、塞 Model、返回视图名。
 * 不写业务规则、不拼查询条件、不做金额计算。
 */
@Controller
public class ProductController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public ProductController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/main")
    public String main(Model model) {
        model.addAttribute("nav", categoryService.getNavigation());
        model.addAttribute("newArrivals", productService.getNewArrivals());
        model.addAttribute("featured", productService.getFeatured());
        return "Main";
    }
}
