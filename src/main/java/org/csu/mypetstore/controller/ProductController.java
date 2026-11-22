package org.csu.mypetstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.entity.Category;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.service.CategoryService;
import org.csu.mypetstore.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品相关页面控制器。每个方法只做三件事：调 Service、塞 Model、返回视图名。
 * 不写业务规则、不拼查询条件、不做金额计算。
 * 方法名按"页面的语义"起，与 URL 无关。
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
public String welcome() {
    return "index";
}

@GetMapping("/main")
public String homePage(Model model) {
    model.addAttribute("nav", categoryService.getNavigation());
    model.addAttribute("newArrivals", productService.getNewArrivals());
    model.addAttribute("featured", productService.getFeatured());
    return "Main";
}

@GetMapping("/category")
public String productList(@RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) String productType,
                          @RequestParam(required = false) String sort,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") long page,
                          @RequestParam(defaultValue = "6") long pageSize,
                          Model model) {
    String type = CategoryService.normalizeProductType(productType);
    String appliedSort = ProductService.normalizeSort(sort);

    List<Long> categoryIds = categoryService.resolveProductCategoryIds(categoryId, type);
    Page<Product> result = productService.search(categoryIds, appliedSort, keyword, page, pageSize);

    Category current = categoryService.findById(categoryId).orElse(null);
    Category parent = (current != null && current.getParentId() != null)
                      ? categoryService.findById(current.getParentId()).orElse(null)
                      : null;

    model.addAttribute("nav", categoryService.getNavigation());
    model.addAttribute("page", result);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("productType", type);
    model.addAttribute("sort", appliedSort);
    model.addAttribute("keyword", keyword == null ? "" : keyword);
    model.addAttribute("pageSize", result.getSize());
    model.addAttribute("currentCategory", current);
    model.addAttribute("parentCategory", parent);
    return "Category";
}

}
