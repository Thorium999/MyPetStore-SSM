package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.dto.Receiver;
import org.csu.mypetstore.entity.CartItem;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.entity.SalesOrder;
import org.csu.mypetstore.mapper.CartItemMapper;
import org.csu.mypetstore.mapper.ProductMapper;
import org.csu.mypetstore.mapper.SalesOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 下单流程测试。整类 @Transactional，跑完回滚，不动种子数据。
 */
@SpringBootTest
@Transactional
class OrderServiceTest {

    private static final Long ALICE = 1L;
    private static final Long BOB = 2L;

    private static final Receiver RECEIVER = new Receiver("林同学", "13800001111", "湖南省长沙市岳麓区某路 1 号");

    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private CartItemMapper cartItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private SalesOrderMapper orderMapper;

    @BeforeEach
    void prepareCatalog() {
        setStock(21L, 100);
        setStock(30L, 12);
        setStatus(30L, "DRAFT");
    }

    @Test
    void rejectsCartWithBlockedLines() {
        clearCart(ALICE);
        addDirectly(ALICE, 30L, 1);   // DRAFT 商品，正常加购加不进来
        long ordersBefore = orderCount();

        assertThatThrownBy(() -> orderService.placeOrder(ALICE, RECEIVER))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("请先处理");

        assertThat(orderCount()).isEqualTo(ordersBefore);
    }

    @Test
    void placesOrderDeductsStockAndClearsCart() {
        clearCart(ALICE);
        cartService.addItem(ALICE, 21L, 2);
        cartService.addItem(ALICE, 22L, 3);

        int stock21Before = stockOf(21L);
        int stock22Before = stockOf(22L);
        BigDecimal expected = priceOf(21L).multiply(BigDecimal.valueOf(2))
                                          .add(priceOf(22L).multiply(BigDecimal.valueOf(3)));

        Long orderId = orderService.placeOrder(ALICE, RECEIVER);
        SalesOrder order = orderMapper.selectById(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderService.STATUS_PENDING_PAYMENT);
        assertThat(order.getOrderNo()).startsWith("ORD");
        assertThat(order.getOrderNo()).hasSizeLessThanOrEqualTo(40);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(expected);
        assertThat(order.getReceiverName()).isEqualTo("林同学");
        assertThat(order.getPaymentNo()).isNull();

        assertThat(orderService.findOrderItems(orderId)).hasSize(2);
        assertThat(orderService.findOrderItems(orderId))
                .allSatisfy(item -> {
                    assertThat(item.getSkuSnapshot()).isNotBlank();
                    assertThat(item.getCoverImageSnapshot()).isNotBlank();
                    assertThat(item.getLineAmount())
                            .isEqualByComparingTo(item.getUnitPrice()
                                                      .multiply(BigDecimal.valueOf(item.getQuantity())));
                });

        assertThat(stockOf(21L)).isEqualTo(stock21Before - 2);
        assertThat(stockOf(22L)).isEqualTo(stock22Before - 3);
        assertThat(cartOf(ALICE)).isEmpty();
    }

    @Test
    void rejectsEmptyCart() {
        clearCart(BOB);

        assertThatThrownBy(() -> orderService.placeOrder(BOB, RECEIVER))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("购物车是空的");
    }

    @Test
    void rejectsInvalidReceiverWithoutTouchingStock() {
        clearCart(ALICE);
        cartService.addItem(ALICE, 21L, 1);
        int stockBefore = stockOf(21L);

        assertThatThrownBy(() -> orderService.placeOrder(ALICE, new Receiver(" ", "13800001111", "地址")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("收货人");
        assertThatThrownBy(() -> orderService.placeOrder(ALICE, new Receiver("林同学", "", "地址")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("联系电话");
        assertThatThrownBy(() -> orderService.placeOrder(ALICE, new Receiver("林同学", "13800001111", "  ")))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("收货地址");

        assertThat(stockOf(21L)).isEqualTo(stockBefore);
        assertThat(cartOf(ALICE)).hasSize(1);
    }

    /**
     * 并发防线：扣库存是带 stock >= quantity 条件的 UPDATE，
     * 库存不够时影响行数为 0，且绝不会把库存扣成负数。
     */
    @Test
    void conditionalUpdateProtectsAgainstOverselling() {
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                                           .set(Product::getStock, 1)
                                           .eq(Product::getId, 21L));

        int affected = productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                                                          .setSql("stock = stock - 2")
                                                          .eq(Product::getId, 21L)
                                                          .eq(Product::getStatus, ProductService.STATUS_ON_SALE)
                                                          .ge(Product::getStock, 2));

        assertThat(affected).isZero();
        assertThat(stockOf(21L)).isEqualTo(1);
    }

    @Test
    void ordersAreVisibleOnlyToTheirOwner() {
        clearCart(ALICE);
        cartService.addItem(ALICE, 21L, 1);
        Long orderId = orderService.placeOrder(ALICE, RECEIVER);

        assertThat(orderService.findOwnOrder(ALICE, orderId)).isPresent();
        assertThat(orderService.findOwnOrder(BOB, orderId)).isEmpty();
        assertThat(orderService.listOrders(ALICE)).anyMatch(o -> o.id().equals(orderId));
        assertThat(orderService.listOrders(BOB)).noneMatch(o -> o.id().equals(orderId));
    }

    @Test
    void statusLabelsAreHumanReadable() {
        assertThat(OrderService.statusLabel("PENDING_PAYMENT")).isEqualTo("待付款");
        assertThat(OrderService.statusLabel("CANCELLED")).isEqualTo("已取消");
        assertThat(OrderService.statusLabel("UNKNOWN")).isEqualTo("UNKNOWN");
    }

    @Test
    void paysPendingOrder() {
        Long orderId = placeSingleItemOrder();

        OrderAction action = orderService.pay(ALICE, orderId);

        assertThat(action.success()).isTrue();
        SalesOrder paid = orderMapper.selectById(orderId);
        assertThat(paid.getStatus()).isEqualTo(OrderService.STATUS_PAID);
        assertThat(paid.getPaymentMethod()).isEqualTo("MOCK");
        assertThat(paid.getPaymentNo()).startsWith("PAY");
        assertThat(paid.getPaidAt()).isNotNull();
        assertThat(paid.getCancelledAt()).isNull();
    }

    @Test
    void cannotPayTwice() {
        Long orderId = placeSingleItemOrder();
        orderService.pay(ALICE, orderId);

        OrderAction second = orderService.pay(ALICE, orderId);

        assertThat(second.success()).isFalse();
        assertThat(second.message()).contains("已支付");
        assertThat(orderMapper.selectById(orderId).getStatus()).isEqualTo(OrderService.STATUS_PAID);
    }

    @Test
    void cancelRestoresStock() {
        int stockBefore = stockOf(21L);
        Long orderId = placeSingleItemOrder();
        assertThat(stockOf(21L)).isEqualTo(stockBefore - 1);

        OrderAction action = orderService.cancel(ALICE, orderId);

        assertThat(action.success()).isTrue();
        SalesOrder cancelled = orderMapper.selectById(orderId);
        assertThat(cancelled.getStatus()).isEqualTo(OrderService.STATUS_CANCELLED);
        assertThat(cancelled.getCancelledAt()).isNotNull();
        assertThat(cancelled.getPaymentNo()).isNull();
        assertThat(stockOf(21L)).isEqualTo(stockBefore);
    }

    @Test
    void cannotCancelPaidOrder() {
        Long orderId = placeSingleItemOrder();
        orderService.pay(ALICE, orderId);

        OrderAction action = orderService.cancel(ALICE, orderId);

        assertThat(action.success()).isFalse();
        assertThat(action.message()).contains("已支付");
        assertThat(orderMapper.selectById(orderId).getStatus()).isEqualTo(OrderService.STATUS_PAID);
    }

    @Test
    void cannotPayCancelledOrder() {
        Long orderId = placeSingleItemOrder();
        orderService.cancel(ALICE, orderId);

        OrderAction action = orderService.pay(ALICE, orderId);

        assertThat(action.success()).isFalse();
        assertThat(action.message()).contains("已取消");
    }

    @Test
    void cannotActOnSomeoneElsesOrder() {
        Long orderId = placeSingleItemOrder();

        assertThat(orderService.pay(BOB, orderId).message()).isEqualTo("订单不存在");
        assertThat(orderService.cancel(BOB, orderId).message()).isEqualTo("订单不存在");
        assertThat(orderMapper.selectById(orderId).getStatus()).isEqualTo(OrderService.STATUS_PENDING_PAYMENT);
    }

    private Long placeSingleItemOrder() {
        clearCart(ALICE);
        cartService.addItem(ALICE, 21L, 1);
        return orderService.placeOrder(ALICE, RECEIVER);
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

    private java.util.List<CartItem> cartOf(Long userId) {
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    }

    private int stockOf(Long productId) {
        return productMapper.selectById(productId).getStock();
    }

    private BigDecimal priceOf(Long productId) {
        return productMapper.selectById(productId).getPrice();
    }

    private long orderCount() {
        return orderMapper.selectCount(null);
    }
}
