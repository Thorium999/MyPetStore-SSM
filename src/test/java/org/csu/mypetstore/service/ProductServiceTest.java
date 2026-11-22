package org.csu.mypetstore.service;

import org.csu.mypetstore.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 首页两个栏目的查询测试（打真库，验证条数上限与排序确实生效）。
 */
@SpringBootTest
class ProductServiceTest {

    private static final String ON_SALE = "ON_SALE";

    @Autowired
    private ProductService productService;

    @Test
    void newArrivalsAreThreeOnSaleProducts() {
        List<Product> products = productService.getNewArrivals();

        assertThat(products).hasSize(3);
        assertThat(products).allSatisfy(p -> assertThat(p.getStatus()).isEqualTo(ON_SALE));
    }

    @Test
    void newArrivalsAreOrderedByCreatedAtDesc() {
        List<Product> products = productService.getNewArrivals();

        assertThat(products).isSortedAccordingTo(
                (a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    }

    @Test
    void featuredAreFourBySortOrder() {
        List<Product> products = productService.getFeatured();

        assertThat(products).hasSize(4);
        assertThat(products).allSatisfy(p -> assertThat(p.getStatus()).isEqualTo(ON_SALE));
        assertThat(products).isSortedAccordingTo(
                (a, b) -> a.getSortOrder().compareTo(b.getSortOrder()));
    }
}
