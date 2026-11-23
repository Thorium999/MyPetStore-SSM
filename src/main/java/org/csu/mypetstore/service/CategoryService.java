package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.dto.CategoryNavigation;
import org.csu.mypetstore.entity.Category;
import org.csu.mypetstore.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

/**
 * 列表页的类型入口。类型不是 product 字段，由二级分类的编码后缀推导
 */
public static final String TYPE_ALL = "ALL";
public static final String TYPE_PETS = "PETS";
public static final String TYPE_SUPPLIES = "SUPPLIES";

private static final String CODE_SUFFIX_PETS = "_PETS";
private static final String CODE_SUFFIX_SUPPLIES = "_SUPPLIES";

private final CategoryMapper categoryMapper;

public CategoryService(CategoryMapper categoryMapper) {
    this.categoryMapper = categoryMapper;
}

/**
 * 首页分类导航：一次查出全部 15 条，按 sort_order、id 升序，
 * 组装成「5 个一级 + 各自 2 个二级」。
 */
public List<CategoryNavigation> getNavigation() {
    List<Category> all = selectAllOrdered();
    
    Map<Long, List<Category>> childrenByParent = all.stream()
                                                    .filter(category -> category.getParentId() != null)
                                                    .collect(Collectors.groupingBy(
                                                            Category::getParentId, LinkedHashMap::new, Collectors.toList()));
    
    return all.stream()
              .filter(category -> category.getParentId() == null)
              .map(top -> new CategoryNavigation(
                      top, childrenByParent.getOrDefault(top.getId(), List.of())))
              .toList();
}

/**
 * 把「一级或二级的 categoryId + 类型」翻译成商品查询要用的二级分类 id 列表。
 * 规则：选一级 → 取它下面所有二级；选二级 → 就它自己；再按类型筛（宠物 / 用品）。
 * 传了不存在的分类返回空列表（页面会呈现"没有符合条件的商品"）。
 */
public List<Long> resolveProductCategoryIds(Long categoryId, String productType) {
    List<Category> all = selectAllOrdered();
    String type = normalizeProductType(productType);
    
    List<Category> candidates;
    if (categoryId == null)
        candidates = all.stream().filter(category -> category.getParentId() != null).toList();
    else {
        Category target = all.stream()
                             .filter(category -> category.getId().equals(categoryId))
                             .findFirst()
                             .orElse(null);
        if (target == null)
            return List.of();
        candidates = target.getParentId() != null
                     ? List.of(target)
                     : all.stream().filter(category -> categoryId.equals(category.getParentId())).toList();
    }
    
    return candidates.stream()
                     .filter(category -> matchesType(category, type))
                     .map(Category::getId)
                     .toList();
}

public Optional<Category> findById(Long categoryId) {
    return categoryId == null ? Optional.empty() : Optional.ofNullable(categoryMapper.selectById(categoryId));
}

public static String normalizeProductType(String raw) {
    if (TYPE_PETS.equalsIgnoreCase(raw))
        return TYPE_PETS;
    if (TYPE_SUPPLIES.equalsIgnoreCase(raw))
        return TYPE_SUPPLIES;
    return TYPE_ALL;
}

/**
 * 由二级分类的编码后缀判断它属于宠物还是用品；一级或未知返回 ALL。
 */
public static String typeOf(Category category) {
    if (category == null || category.getCode() == null)
        return TYPE_ALL;
    String code = category.getCode();
    if (code.endsWith(CODE_SUFFIX_SUPPLIES))
        return TYPE_SUPPLIES;
    if (code.endsWith(CODE_SUFFIX_PETS))
        return TYPE_PETS;
    return TYPE_ALL;
}

private List<Category> selectAllOrdered() {
    return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                                             .orderByAsc(Category::getSortOrder)
                                             .orderByAsc(Category::getId));
}

private boolean matchesType(Category category, String productType) {
    String code = category.getCode() == null ? "" : category.getCode();
    return switch (productType) {
        case TYPE_PETS -> code.endsWith(CODE_SUFFIX_PETS);
        case TYPE_SUPPLIES -> code.endsWith(CODE_SUFFIX_SUPPLIES);
        default -> true;
    };
}

}
