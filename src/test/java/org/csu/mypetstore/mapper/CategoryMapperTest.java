package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分类数据访问的集成测试（连 dev 数据源打真库）。
 * 断言的是种子数据的既定事实：5 个一级 + 10 个二级。
 */
@SpringBootTest
class CategoryMapperTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void countsAllCategories() {
        assertThat(categoryMapper.selectCount(null)).isEqualTo(15L);
    }

    @Test
    void splitsIntoFiveTopLevelAndTenChildren() {
        List<Category> all = categoryMapper.selectList(null);

        List<Category> topLevel = all.stream().filter(c -> c.getParentId() == null).toList();
        List<Category> children = all.stream().filter(c -> c.getParentId() != null).toList();

        assertThat(topLevel).hasSize(5);
        assertThat(children).hasSize(10);
        assertThat(topLevel).allSatisfy(c -> assertThat(c.getCode()).isNotBlank());
    }

    @Test
    void ordersTopLevelBySortOrder() {
        List<Category> topLevel = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .isNull(Category::getParentId)
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId));

        assertThat(topLevel).hasSize(5);
        assertThat(topLevel.get(0).getCode()).isEqualTo("DOGS");
        assertThat(topLevel.get(topLevel.size() - 1).getCode()).isEqualTo("REPTILES");
    }

    @Test
    void childrenOfDogsArePetsAndSupplies() {
        List<Category> children = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getParentId, 1L)
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId));

        assertThat(children).extracting(Category::getCode)
                .containsExactly("DOGS_PETS", "DOGS_SUPPLIES");
        assertThat(children).extracting(Category::getName)
                .containsExactly("宠物", "用品");
    }
}
