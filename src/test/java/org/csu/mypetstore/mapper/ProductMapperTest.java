package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品数据访问的集成测试（连 dev 数据源打真库）。
 */
@SpringBootTest
class ProductMapperTest {

    private static final String ON_SALE = "ON_SALE";
    private static final String DRAFT = "DRAFT";

    @Autowired
    private ProductMapper productMapper;

    @Test
    void countsAllProducts() {
        assertThat(productMapper.selectCount(null)).isEqualTo(60L);
    }

    @Test
    void countsByStatus() {
        assertThat(productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getStatus, ON_SALE))).isEqualTo(54L);
        assertThat(productMapper.selectCount(
                Wrappers.<Product>lambdaQuery().eq(Product::getStatus, DRAFT))).isEqualTo(6L);
    }

    @Test
    void readsPriceWithoutLosingScale() {
        Product leash = productMapper.selectOne(
                Wrappers.<Product>lambdaQuery().eq(Product::getSku, "K9-LEASH-01"));

        assertThat(leash).isNotNull();
        assertThat(leash.getPrice()).isEqualByComparingTo(new BigDecimal("49.90"));
        assertThat(leash.getCoverImage()).startsWith("images/");
    }

    @Test
    void newArrivalsQueryReturnsThreeOnSaleProducts() {
        List<Product> newest = productMapper.selectList(
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getStatus, ON_SALE)
                        .orderByDesc(Product::getCreatedAt)
                        .orderByDesc(Product::getId)
                        .last("LIMIT 3"));

        assertThat(newest).hasSize(3);
        assertThat(newest).allSatisfy(p -> assertThat(p.getStatus()).isEqualTo(ON_SALE));
    }

    @Test
    void featuredQueryReturnsFourBySortOrder() {
        List<Product> featured = productMapper.selectList(
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getStatus, ON_SALE)
                        .orderByAsc(Product::getSortOrder)
                        .orderByAsc(Product::getId)
                        .last("LIMIT 4"));

        assertThat(featured).hasSize(4);
        assertThat(featured).isSortedAccordingTo(
                (a, b) -> a.getSortOrder().compareTo(b.getSortOrder()));
    }
}
