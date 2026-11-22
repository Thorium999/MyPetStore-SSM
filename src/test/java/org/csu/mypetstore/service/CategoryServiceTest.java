package org.csu.mypetstore.service;

import org.csu.mypetstore.dto.CategoryNavigation;
import org.csu.mypetstore.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分类导航的测试。
 *
 * 这里故意打真库（@SpringBootTest）而不是用 Mockito：
 * 要验证的正是"排序 + 分组"这段查询与组装，Mock 掉 Mapper 就等于把被测对象替换成假的。
 * Mockito 的用法见 UserServiceTest。
 */
@SpringBootTest
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void returnsFiveTopLevelNodes() {
        List<CategoryNavigation> nav = categoryService.getNavigation();

        assertThat(nav).hasSize(5);
        assertThat(nav).extracting(node -> node.category().getCode())
                .containsExactly("DOGS", "CATS", "FISH", "BIRDS", "REPTILES");
    }

    @Test
    void everyTopLevelHasTwoChildren() {
        List<CategoryNavigation> nav = categoryService.getNavigation();

        assertThat(nav).allSatisfy(node -> assertThat(node.children()).hasSize(2));
        assertThat(nav.get(0).children())
                .extracting(Category::getName)
                .containsExactly("宠物", "用品");
    }

    @Test
    void childrenBelongToTheirParent() {
        List<CategoryNavigation> nav = categoryService.getNavigation();

        assertThat(nav).allSatisfy(node -> assertThat(node.children())
                .allSatisfy(child -> assertThat(child.getParentId()).isEqualTo(node.category().getId())));
    }

    @Test
    void resolvesAllSecondLevelIdsWhenNoCategoryGiven() {
        assertThat(categoryService.resolveProductCategoryIds(null, CategoryService.TYPE_ALL)).hasSize(10);
    }

    @Test
    void resolvesTopLevelToBothChildren() {
        assertThat(categoryService.resolveProductCategoryIds(1L, CategoryService.TYPE_ALL))
                .containsExactly(101L, 102L);
    }

    @Test
    void resolvesTopLevelWithPetsType() {
        assertThat(categoryService.resolveProductCategoryIds(1L, CategoryService.TYPE_PETS))
                .containsExactly(101L);
    }

    @Test
    void resolvesTopLevelWithSuppliesType() {
        assertThat(categoryService.resolveProductCategoryIds(1L, CategoryService.TYPE_SUPPLIES))
                .containsExactly(102L);
    }

    @Test
    void resolvesSecondLevelToItself() {
        assertThat(categoryService.resolveProductCategoryIds(101L, CategoryService.TYPE_ALL))
                .containsExactly(101L);
    }

    @Test
    void resolvesPetsTypeAcrossAllTopLevels() {
        assertThat(categoryService.resolveProductCategoryIds(null, CategoryService.TYPE_PETS)).hasSize(5);
    }

    @Test
    void returnsEmptyForUnknownCategory() {
        assertThat(categoryService.resolveProductCategoryIds(999L, CategoryService.TYPE_ALL)).isEmpty();
    }

    @Test
    void normalizesUnknownProductTypeToAll() {
        assertThat(CategoryService.normalizeProductType("whatever")).isEqualTo(CategoryService.TYPE_ALL);
        assertThat(CategoryService.normalizeProductType(null)).isEqualTo(CategoryService.TYPE_ALL);
        assertThat(CategoryService.normalizeProductType("pets")).isEqualTo(CategoryService.TYPE_PETS);
    }
}
