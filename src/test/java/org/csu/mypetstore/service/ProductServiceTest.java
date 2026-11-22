package org.csu.mypetstore.service;

import org.junit.jupiter.api.Disabled;

/**
 * 商品业务的单元测试（Mockito，不连库）。
 *
 * 待办：
 * 1. @Mock ProductMapper + @InjectMocks ProductService
 * 2. 断言 getNewArrivals() 只取 3 条、且查询条件是 ON_SALE + created_at 倒序
 * 3. 断言 getFeatured() 取 4 条、按 sort_order 升序
 * 4. 列表页分页完成后，补「类型 = PETS/SUPPLIES 由二级分类推导」的用例
 */
@Disabled("骨架占位，Service 实现后补用例")
class ProductServiceTest {
}
