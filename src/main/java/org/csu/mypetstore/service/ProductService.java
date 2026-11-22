package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

/**
 * 前台只展示上架商品
 */
private static final String ON_SALE = "ON_SALE";

/**
 * 首页两个栏目的条数上限
 */
private static final int NEW_ARRIVAL_LIMIT = 3;
private static final int FEATURED_LIMIT = 4;

private final ProductMapper productMapper;

public ProductService(ProductMapper productMapper) {
    this.productMapper = productMapper;
}

/**
 * 新品上架：最近创建的 3 件
 */
public List<Product> getNewArrivals() {
    return productMapper.selectList(
            Wrappers.<Product>lambdaQuery()
                    .eq(Product::getStatus, ON_SALE)
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
                    .eq(Product::getStatus, ON_SALE)
                    .orderByAsc(Product::getSortOrder)
                    .orderByAsc(Product::getId)
                    .last("LIMIT " + FEATURED_LIMIT));
}

}
