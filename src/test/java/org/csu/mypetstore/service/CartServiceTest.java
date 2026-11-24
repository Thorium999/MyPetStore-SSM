package org.csu.mypetstore.service;

import org.csu.mypetstore.dto.CartLine;
import org.csu.mypetstore.dto.CartSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 购物车业务测试。打真库但整类 @Transactional，跑完回滚，不会污染种子数据。
 * 用 bob（id=2）做写操作，alice（id=1）只看不改。
 */
@SpringBootTest
@Transactional
class CartServiceTest {

    /** alice 的种子购物车：product 6 超库存、30 已下架、34 售罄 */
    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;

    /** bob 车里没有的商品，用来测加购 */
    private static final Long FREE_PRODUCT = 22L;
    private static final Long SOLD_OUT_PRODUCT = 34L;
    private static final Long DRAFT_PRODUCT = 30L;

    @Autowired
    private CartService cartService;

    @Test
    void readsSeedCartWithAllEdgeStates() {
        CartSnapshot cart = cartService.getCart(ALICE);

        assertThat(cart.lines()).hasSize(6);
        assertThat(cart.itemCount()).isEqualTo(10);

        assertThat(cart.lines()).extracting(CartLine::issue)
                .contains("已下架", "已售罄", "库存仅 1 件");
        assertThat(cart.hasBlockedLine()).isTrue();
    }

    @Test
    void totalOnlyCountsPurchasableLines() {
        CartSnapshot cart = cartService.getCart(ALICE);

        BigDecimal expected = cart.lines().stream()
                                  .filter(CartLine::purchasable)
                                  .map(CartLine::subtotal)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(cart.totalAmount()).isEqualByComparingTo(expected);
        assertThat(cart.totalAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void addItemInsertsNewLine() {
        assertThat(cartService.addItem(BOB, FREE_PRODUCT, 2)).isEqualTo("已加入购物车");

        CartLine line = findLine(BOB, FREE_PRODUCT);
        assertThat(line.quantity()).isEqualTo(2);
        assertThat(line.purchasable()).isTrue();
    }

    @Test
    void addSameItemMergesQuantity() {
        cartService.addItem(BOB, FREE_PRODUCT, 2);
        String message = cartService.addItem(BOB, FREE_PRODUCT, 3);

        assertThat(message).contains("现有 5 件");
        assertThat(findLine(BOB, FREE_PRODUCT).quantity()).isEqualTo(5);
    }

    @Test
    void addItemClampsToStock() {
        // 商品 71（布偶猫）库存 3
        cartService.addItem(BOB, 71L, 99);

        assertThat(findLine(BOB, 71L).quantity()).isEqualTo(3);
    }

    @Test
    void rejectsSoldOutAndDraftProducts() {
        assertThat(cartService.addItem(BOB, SOLD_OUT_PRODUCT, 1)).isEqualTo("商品已售罄");
        assertThat(cartService.addItem(BOB, DRAFT_PRODUCT, 1)).isEqualTo("商品不存在或已下架");
        assertThat(cartService.addItem(BOB, 99999L, 1)).isEqualTo("商品不存在或已下架");
    }

    @Test
    void updateQuantityClampsToStock() {
        cartService.addItem(BOB, FREE_PRODUCT, 2);
        CartLine line = findLine(BOB, FREE_PRODUCT);

        // 商品 22 库存 73，要 200 件应被夹到库存值
        assertThat(cartService.updateQuantity(BOB, line.itemId(), 200)).contains("库存只有 73 件");
        assertThat(findLine(BOB, FREE_PRODUCT).quantity()).isEqualTo(line.stock());
    }

    @Test
    void updateQuantityClampsToPerOrderLimitWhenStockIsLarger() {
        // 商品 28 库存 119：上限是单次 99，不该说成"库存只有 99 件"
        cartService.addItem(BOB, 28L, 1);
        CartLine line = findLine(BOB, 28L);

        String message = cartService.updateQuantity(BOB, line.itemId(), 150);

        assertThat(message).contains("单次最多购买 99 件");
        assertThat(message).doesNotContain("库存只有");
        assertThat(findLine(BOB, 28L).quantity()).isEqualTo(CartService.MAX_QUANTITY);
    }

    @Test
    void updateQuantityRejectsForeignItem() {
        CartLine aliceLine = cartService.getCart(ALICE).lines().get(0);

        assertThat(cartService.updateQuantity(BOB, aliceLine.itemId(), 5)).isEqualTo("购物车条目不存在");
        assertThat(cartService.getCart(ALICE).lines().get(0).quantity()).isEqualTo(aliceLine.quantity());
    }

    @Test
    void removeItemOnlyAffectsOwner() {
        CartLine aliceLine = cartService.getCart(ALICE).lines().get(0);

        assertThat(cartService.removeItem(BOB, aliceLine.itemId())).isFalse();
        assertThat(cartService.removeItem(ALICE, aliceLine.itemId())).isTrue();
        assertThat(cartService.getCart(ALICE).lines()).hasSize(5);
    }

    @Test
    void countsQuantitiesForHeaderBadge() {
        assertThat(cartService.countItems(ALICE)).isEqualTo(10);
        assertThat(cartService.countItems(999L)).isZero();
    }

    private CartLine findLine(Long userId, Long productId) {
        return cartService.getCart(userId).lines().stream()
                          .filter(line -> line.productId().equals(productId))
                          .findFirst()
                          .orElseThrow();
    }
}
