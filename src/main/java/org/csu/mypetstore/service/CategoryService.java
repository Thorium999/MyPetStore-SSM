package org.csu.mypetstore.service;

import org.springframework.stereotype.Service;

/**
 * 分类业务。
 *
 * 待办（首页用）：
 * 1. 注入 CategoryMapper
 * 2. getNavigation()：一次查出全部 15 条，按 sort_order、id 升序，
 *    组装成 List<CategoryNavigation>——只返回 5 个一级，二级挂在各自父节点下
 *
 * 待办（列表页用，后续）：
 * 3. 提供一级分类校验 / 二级分类校验（商品必须挂二级）
 */
@Service
public class CategoryService {
}
