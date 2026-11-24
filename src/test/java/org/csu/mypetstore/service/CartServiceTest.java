package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.dto.CartLine;
import org.csu.mypetstore.dto.CartSnapshot;
import org.csu.mypetstore.entity.CartItem;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.mapper.CartItemMapper;
import org.csu.mypetstore.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 购物车业务测试。整类 @Transactional（跑完回滚），且每个用例先清空再自建数据，
 * 不依赖种子购物车——否则谁在页面上点两下，测试就红了。
 */
@SpringBootTest
@Transactional
class CartServiceTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;

    /** 库存 41 */
    private static final Long NORMAL_PRODUCT = 21L;
    /** 库存 73 */
    private static final Long ANOTHER_PRODUCT = 22L;
    /** 库存 3 */
    private static final Long LOW_STOCK_PRODUCT = 71L;
    /** 库存大于单次上限 99 的商品 */
    private static final Long HUGE_STOCK_PRODUCT = 28L;
    /** 库存 1，用来构造"数量超过库存" */
    private static final Long TIGHT_STOCK_PRODUCT = 6L;
    /** DRAFT */
    private static final Long DRAFT_PRODUCT = 30L;
    /** 库存 0 */
    private static final Long SOLD_OUT_PRODUCT = 34L;

    @Autowired
    private CartService cartService;
    @Autowired
    private CartItemMapper cartItemMapper;
    @Autowired
    private ProductMapper productMapper;

    @BeforeEach
    void prepareCatalog() {
        setStock(NORMAL_PRODUCT, 41);
        setStock(ANOTHER_PRODUCT, 73);
        setStock(LOW_STOCK_PRODUCT, 3);
        setStock(HUGE_STOCK_PRODUCT, 119);
        setStock(TIGHT_STOCK_PRODUCT, 1);
        setStock(SOLD_OUT_PRODUCT, 0);
        setStatus(DRAFT_PRODUCT, "DRAFT");
    }

    @Test
    void marksEachBlockedLineWithItsOwnReason() {
        clearCart(ALICE);
        addDirectly(ALICE, DRAFT_PRODUCT, 1);
        addDirectly(ALICE, SOLD_OUT_PRODUCT, 1);
        addDirectly(ALICE, TIGHT_STOCK_PRODUCT, 2);
        addDirectly(ALICE, NORMAL_PRODUCT, 1);

        CartSnapshot cart = cartService.getCart(ALICE);

        assertThat(cart.lines()).hasSize(4);
        assertThat(cart.lines()).extracting(CartLine::issue)
                .contains("已下架", "已售罄", "库存仅 1 件", "");
        assertThat(cart.hasBlockedLine()).isTrue();
        assertThat(cart.itemCount()).isEqualTo(5);
    }

    @Test
    void totalOnlyCountsPurchasableLines() {
        clearCart(ALICE);
        addDirectly(ALICE, NORMAL_PRODUCT, 2);
        addDirectly(ALICE, ANOTHER_PRODUCT, 3);
        addDirectly(ALICE, SOLD_OUT_PRODUCT, 1);

        CartSnapshot cart = cartService.getCart(ALICE);
        BigDecimal expected = cart.lines().stream()
                                  .filter(CartLine::purchasable)
                                  .map(CartLine::subtotal)
                                  .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(cart.totalAmount()).isEqualByComparingTo(expected);
        assertThat(cart.totalAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(cart.lines().stream().filter(line -> !line.purchasable()).count()).isEqualTo(1);
    }

    @Test
    void addItemInsertsNewLine() {
        clearCart(BOB);

        assertThat(cartService.addItem(BOB, ANOTHER_PRODUCT, 2)).isEqualTo("已加入购物车");

        CartLine line = findLine(BOB, ANOTHER_PRODUCT);
        assertThat(line.quantity()).isEqualTo(2);
        assertThat(line.purchasable()).isTrue();
    }

    @Test
    void addSameItemMergesQuantity() {
        clearCart(BOB);
        cartService.addItem(BOB, ANOTHER_PRODUCT, 2);

        String message = cartService.addItem(BOB, ANOTHER_PRODUCT, 3);

        assertThat(message).contains("现有 5 件");
        assertThat(findLine(BOB, ANOTHER_PRODUCT).quantity()).isEqualTo(5);
    }

    @Test
    void addItemClampsToStock() {
        clearCart(BOB);

        cartService.addItem(BOB, LOW_STOCK_PRODUCT, 99);

        assertThat(findLine(BOB, LOW_STOCK_PRODUCT).quantity()).isEqualTo(3);
    }

    @Test
    void rejectsSoldOutAndDraftProducts() {
        clearCart(BOB);

        assertThat(cartService.addItem(BOB, SOLD_OUT_PRODUCT, 1)).isEqualTo("商品已售罄");
        assertThat(cartService.addItem(BOB, DRAFT_PRODUCT, 1)).isEqualTo("商品不存在或已下架");
        assertThat(cartService.addItem(BOB, 99999L, 1)).isEqualTo("商品不存在或已下架");
    }

    @Test
    void updateQuantityClampsToStock() {
        clearCart(BOB);
        cartService.addItem(BOB, ANOTHER_PRODUCT, 2);
        CartLine line = findLine(BOB, ANOTHER_PRODUCT);

        // 库存 73：要 200 件应被夹到库存，并说明是库存不足
        assertThat(cartService.updateQuantity(BOB, line.itemId(), 200)).contains("库存只有");
        assertThat(findLine(BOB, ANOTHER_PRODUCT).quantity()).isEqualTo(73);
    }

    @Test
    void updateQuantityClampsToPerOrderLimitWhenStockIsLarger() {
        clearCart(BOB);
        cartService.addItem(BOB, HUGE_STOCK_PRODUCT, 1);
        CartLine line = findLine(BOB, HUGE_STOCK_PRODUCT);

        // 库存大于 99 时，上限来自"单次最多"，不能说成库存只有 99 件
        String message = cartService.updateQuantity(BOB, line.itemId(), 150);

        assertThat(message).contains("单次最多购买 99 件");
        assertThat(message).doesNotContain("库存只有");
        assertThat(findLine(BOB, HUGE_STOCK_PRODUCT).quantity()).isEqualTo(CartService.MAX_QUANTITY);
    }

    @Test
    void updateQuantityRejectsForeignItem() {
        clearCart(ALICE);
        clearCart(BOB);
        addDirectly(ALICE, NORMAL_PRODUCT, 1);
        CartLine aliceLine = cartService.getCart(ALICE).lines().get(0);

        assertThat(cartService.updateQuantity(BOB, aliceLine.itemId(), 5)).isEqualTo("购物车条目不存在");
        assertThat(cartService.getCart(ALICE).lines().get(0).quantity()).isEqualTo(1);
    }

    @Test
    void removeItemOnlyAffectsOwner() {
        clearCart(ALICE);
        addDirectly(ALICE, NORMAL_PRODUCT, 1);
        addDirectly(ALICE, ANOTHER_PRODUCT, 1);
        CartLine aliceLine = cartService.getCart(ALICE).lines().get(0);

        assertThat(cartService.removeItem(BOB, aliceLine.itemId())).isFalse();
        assertThat(cartService.removeItem(ALICE, aliceLine.itemId())).isTrue();
        assertThat(cartService.getCart(ALICE).lines()).hasSize(1);
    }

    @Test
    void countsQuantitiesForHeaderBadge() {
        clearCart(BOB);
        addDirectly(BOB, NORMAL_PRODUCT, 2);
        addDirectly(BOB, ANOTHER_PRODUCT, 3);

        assertThat(cartService.countItems(BOB)).isEqualTo(5);
        assertThat(cartService.countItems(999L)).isZero();
    }


    /** 手动把商品调成已知状态：谁在页面上买空了货，测试也不受影响（事务结束自动回滚）。 */
    private void setStock(Long productId, int stock) {
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getStock, stock)
                .eq(Product::getId, productId));
    }

    private void setStatus(Long productId, String status) {
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .set(Product::getStatus, status)
                .eq(Product::getId, productId));
    }

    /** 绕过 addItem 的校验直接插行：用来构造"下架 / 售罄 / 超库存"这类页面上做不出来的状态。 */
    private void addDirectly(Long userId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setVersion(0L);
        cartItemMapper.insert(item);
    }

    private void clearCart(Long userId) {
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }

    private CartLine findLine(Long userId, Long productId) {
        return cartService.getCart(userId).lines().stream()
                          .filter(line -> line.productId().equals(productId))
                          .findFirst()
                          .orElseThrow();
    }
}
