package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.entity.ProductImage;
import org.csu.mypetstore.mapper.ProductImageMapper;
import org.csu.mypetstore.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

/**
 * 前台只展示上架商品
 */
public static final String STATUS_ON_SALE = "ON_SALE";

/**
 * 列表页排序方式
 */
public static final String SORT_DEFAULT = "DEFAULT";
public static final String SORT_PRICE_ASC = "PRICE_ASC";
public static final String SORT_PRICE_DESC = "PRICE_DESC";
public static final String SORT_NEWEST = "NEWEST";

/**
 * 列表页每页条数：默认 6，允许 1—50
 */
public static final int DEFAULT_PAGE_SIZE = 6;
public static final int MAX_PAGE_SIZE = 50;

/**
 * 单次购买数量上限（PRD：1—99）
 */
public static final int MAX_QUANTITY = 99;

/**
 * 首页两个栏目的条数上限
 */
private static final int NEW_ARRIVAL_LIMIT = 3;
private static final int FEATURED_LIMIT = 4;

private final ProductMapper productMapper;
private final ProductImageMapper productImageMapper;

public ProductService(ProductMapper productMapper, ProductImageMapper productImageMapper) {
    this.productMapper = productMapper;
    this.productImageMapper = productImageMapper;
}

/**
 * 新品上架：最近创建的 3 件
 */
public List<Product> getNewArrivals() {
    return productMapper.selectList(
            Wrappers.<Product>lambdaQuery()
                    .eq(Product::getStatus, STATUS_ON_SALE)
                    .orderByDesc(Product::getCreatedAt)
                    .orderByDesc(Product::getId)
                    .last("LIMIT " + NEW_ARRIVAL_LIMIT));
}

/**
 * 精选推荐：人工指定前 4 件，不做推荐算法
 */
public List<Product> getFeatured() {
    return productMapper.selectList(
            Wrappers.<Product>lambdaQuery()
                    .eq(Product::getStatus, STATUS_ON_SALE)
                    .orderByAsc(Product::getSortOrder)
                    .orderByAsc(Product::getId)
                    .last("LIMIT " + FEATURED_LIMIT));
}

/**
 * 列表页分页查询。categoryIds 由 CategoryService 按「分类 + 类型」翻译好传进来，
 * 这里只负责拼查询条件、排序与分页；传空列表表示"这个组合下没有分类"，直接返回空页。
 */
public Page<Product> search(List<Long> categoryIds, String sort, String keyword, long page, long pageSize) {
    long safePage = Math.max(page, 1);
    long safePageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE :
                        Math.min(pageSize, MAX_PAGE_SIZE);
    Page<Product> pageRequest = new Page<>(safePage, safePageSize);
    
    if (categoryIds != null && categoryIds.isEmpty()) {
        pageRequest.setRecords(List.of());
        pageRequest.setTotal(0);
        return pageRequest;
    }
    
    LambdaQueryWrapper<Product> query = Wrappers.<Product>lambdaQuery()
                                                .eq(Product::getStatus, STATUS_ON_SALE);
    if (categoryIds != null)
        query.in(Product::getCategoryId, categoryIds);
    if (keyword != null && !keyword.isBlank())
        query.like(Product::getName, keyword.trim());
    
    applySort(query, sort);
    return productMapper.selectPage(pageRequest, query);
}

/**
 * 详情页：按 id 取上架商品。下架或不存在都返回空，由 Controller 转 404，
 * 避免用户拿旧链接直接看到草稿商品。
 */
public Optional<Product> findOnSaleById(Long productId) {
    if (productId == null)
        return Optional.empty();
    
    Product product = productMapper.selectById(productId);
    if (product == null || !STATUS_ON_SALE.equals(product.getStatus()))
        return Optional.empty();
    
    return Optional.of(product);
}

/**
 * 详情页相册：按 sort_order、id 升序。没有相册的商品返回空列表，
 * 模板回退为只显示封面。
 */
public List<ProductImage> getImages(Long productId) {
    if (productId == null)
        return List.of();
    
    return productImageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                                                 .eq(ProductImage::getProductId, productId)
                                                 .orderByAsc(ProductImage::getSortOrder)
                                                 .orderByAsc(ProductImage::getId));
}

public static String normalizeSort(String raw) {
    if (raw == null)
        return SORT_DEFAULT;
    
    return switch (raw) {
        case SORT_PRICE_ASC -> SORT_PRICE_ASC;
        case SORT_PRICE_DESC -> SORT_PRICE_DESC;
        case SORT_NEWEST -> SORT_NEWEST;
        default -> SORT_DEFAULT;
    };
}

private void applySort(LambdaQueryWrapper<Product> query, String sort) {
    switch (normalizeSort(sort)) {
        case SORT_PRICE_ASC -> query.orderByAsc(Product::getPrice).orderByAsc(Product::getId);
        case SORT_PRICE_DESC -> query.orderByDesc(Product::getPrice).orderByAsc(Product::getId);
        case SORT_NEWEST -> query.orderByDesc(Product::getCreatedAt).orderByDesc(Product::getId);
        default -> query.orderByAsc(Product::getSortOrder).orderByAsc(Product::getId);
    }
}

}
