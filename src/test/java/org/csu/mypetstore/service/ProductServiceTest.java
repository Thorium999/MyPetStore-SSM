package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csu.mypetstore.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品业务测试（打真库，验证条数上限、排序、筛选与分页确实生效）。
 */
@SpringBootTest
class ProductServiceTest {

    private static final String ON_SALE = ProductService.STATUS_ON_SALE;

    /** 种子数据：60 个商品，其中 54 个上架 */
    private static final long ON_SALE_TOTAL = 54L;

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

    @Test
    void searchDefaultsToSixPerPage() {
        Page<Product> result = productService.search(null, null, null, 1, 0);

        assertThat(result.getSize()).isEqualTo(ProductService.DEFAULT_PAGE_SIZE);
        assertThat(result.getRecords()).hasSize(ProductService.DEFAULT_PAGE_SIZE);
        assertThat(result.getTotal()).isEqualTo(ON_SALE_TOTAL);
        assertThat(result.getPages()).isEqualTo(ON_SALE_TOTAL / ProductService.DEFAULT_PAGE_SIZE);
    }

    @Test
    void searchClampsPageSizeToUpperBound() {
        Page<Product> result = productService.search(null, null, null, 1, 999);

        assertThat(result.getSize()).isEqualTo(ProductService.MAX_PAGE_SIZE);
    }

    @Test
    void searchClampsPageToOne() {
        Page<Product> result = productService.search(null, null, null, -5, 6);

        assertThat(result.getCurrent()).isEqualTo(1);
    }

    @Test
    void searchSortsByPriceAscending() {
        Page<Product> result = productService.search(null, ProductService.SORT_PRICE_ASC, null, 1, 6);
        List<Product> records = result.getRecords();

        assertThat(records).isSortedAccordingTo(
                (a, b) -> a.getPrice().compareTo(b.getPrice()));
    }

    @Test
    void searchSortsByPriceDescending() {
        Page<Product> result = productService.search(null, ProductService.SORT_PRICE_DESC, null, 1, 6);
        List<Product> records = result.getRecords();

        assertThat(records).isSortedAccordingTo(
                (a, b) -> b.getPrice().compareTo(a.getPrice()));
        assertThat(records.get(0).getPrice()).isGreaterThan(records.get(1).getPrice());
    }

    @Test
    void searchFiltersByKeywordOnName() {
        String keyword = "猫";
        Page<Product> result = productService.search(null, null, keyword, 1, 50);

        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getRecords()).allSatisfy(p -> assertThat(p.getName()).contains(keyword));
    }

    @Test
    void searchReturnsEmptyPageWhenNoCategoryMatches() {
        Page<Product> result = productService.search(List.of(), null, null, 1, 6);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    void searchOnlyReturnsOnSaleProducts() {
        Page<Product> result = productService.search(null, null, null, 1, 50);

        assertThat(result.getRecords()).allSatisfy(p -> assertThat(p.getStatus()).isEqualTo(ON_SALE));
    }
}
