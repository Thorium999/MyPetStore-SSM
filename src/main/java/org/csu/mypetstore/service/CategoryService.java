package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.dto.CategoryNavigation;
import org.csu.mypetstore.entity.Category;
import org.csu.mypetstore.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * 首页分类导航：一次查出全部 15 条，按 sort_order、id 升序，
     * 组装成「5 个一级 + 各自 2 个二级」。
     */
    public List<CategoryNavigation> getNavigation() {
        List<Category> all = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId));

        Map<Long, List<Category>> childrenByParent = all.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(
                        Category::getParentId, LinkedHashMap::new, Collectors.toList()));

        return all.stream()
                .filter(category -> category.getParentId() == null)
                .map(top -> new CategoryNavigation(
                        top,
                        childrenByParent.getOrDefault(top.getId(), List.of())))
                .toList();
    }
}
