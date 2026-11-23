package org.csu.mypetstore.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.entity.Category;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.service.CategoryService;
import org.csu.mypetstore.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

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

    // 页面状态统一成「一级分类 + 类型」：从首页点二级进来（如 101）时，
    // 折算成 犬类 + 宠物，这样筛选按钮的选中态和 URL 都能对上。
    Category selected = categoryService.findById(categoryId).orElse(null);
    Long activeTopId = null;
    if (selected != null) {
        if (selected.getParentId() == null) {
            activeTopId = selected.getId();
        } else {
            activeTopId = selected.getParentId();
            type = CategoryService.typeOf(selected);
        }
    }

    List<Long> categoryIds = categoryService.resolveProductCategoryIds(activeTopId, type);
    Page<Product> result = productService.search(categoryIds, appliedSort, keyword, page, pageSize);

    model.addAttribute("nav", categoryService.getNavigation());
    model.addAttribute("page", result);
    model.addAttribute("activeTopId", activeTopId);
    model.addAttribute("currentCategory", categoryService.findById(activeTopId).orElse(null));
    model.addAttribute("productType", type);
    model.addAttribute("sort", appliedSort);
    model.addAttribute("keyword", keyword == null ? "" : keyword);
    model.addAttribute("pageSize", result.getSize());
    return "Category";
}

@GetMapping("/product")
public String productDetail(@RequestParam Long id, Model model) {
    Product product = productService.findOnSaleById(id)
                                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在或已下架"));

    Category category = categoryService.findById(product.getCategoryId()).orElse(null);
    Category parent = (category != null && category.getParentId() != null)
                      ? categoryService.findById(category.getParentId()).orElse(null)
                      : null;

    int stock = product.getStock() == null ? 0 : product.getStock();

    model.addAttribute("product", product);
    model.addAttribute("images", productService.getImages(product.getId()));
    model.addAttribute("category", category);
    model.addAttribute("parentCategory", parent);
    model.addAttribute("soldOut", stock == 0);
    model.addAttribute("maxQuantity", Math.min(stock, ProductService.MAX_QUANTITY));
    return "Product";
}

}
