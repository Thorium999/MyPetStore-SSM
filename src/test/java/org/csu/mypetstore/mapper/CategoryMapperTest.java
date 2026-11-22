package org.csu.mypetstore.mapper;

import org.junit.jupiter.api.Disabled;

/**
 * 分类数据访问的集成测试（连 dev 数据源打真库）。
 *
 * 待办：
 * 1. @SpringBootTest 注入 CategoryMapper
 * 2. 断言 selectList(null) 返回 15 条
 * 3. 断言按 sort_order、id 升序时第一个是一级分类「犬类」
 */
@Disabled("骨架占位，补齐实体字段后再实现")
class CategoryMapperTest {
}
