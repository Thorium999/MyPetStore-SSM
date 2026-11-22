package org.csu.mypetstore.service;

import org.junit.jupiter.api.Disabled;

/**
 * 分类业务的单元测试（Mockito，不连库）。
 *
 * 待办：
 * 1. @Mock CategoryMapper + @InjectMocks CategoryService
 * 2. 造 5 条一级 + 10 条二级的假数据，断言 getNavigation() 返回 5 个节点、每个含 2 个子节点
 * 3. 断言按 sort_order、id 升序（造乱序数据验证排序）
 */
@Disabled("骨架占位，Service 实现后补用例")
class CategoryServiceTest {
}
