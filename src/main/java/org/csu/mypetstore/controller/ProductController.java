package org.csu.mypetstore.controller;

import org.springframework.stereotype.Controller;

/**
 * 商品相关页面控制器（商铺的三个页面都归它）。
 *
 * 待办——每个方法只做三件事：调 Service、塞 Model、返回视图名：
 *
 *   GET /             → 返回 "index"（欢迎页，点进去到 /main）
 *   GET /index        → 同上（两个路径都能进）
 *   GET /main         → nav / newArrivals / featured 塞 Model，返回 "Main"
 *   GET /category     → 参数 categoryId, productType, sort, keyword, page, pageSize，
 *                       返回 "Category"
 *   GET /product      → 参数 id，返回 "Product"（附带相册）
 *
 * 禁止：Controller 里不写业务规则、不拼查询条件、不做金额/库存计算。
 */
@Controller
public class ProductController {
}
