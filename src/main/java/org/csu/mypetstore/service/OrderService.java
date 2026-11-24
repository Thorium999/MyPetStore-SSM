package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.dto.CartSnapshot;
import org.csu.mypetstore.dto.OrderSummary;
import org.csu.mypetstore.dto.Receiver;
import org.csu.mypetstore.entity.CartItem;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.entity.SalesOrder;
import org.csu.mypetstore.entity.SalesOrderItem;
import org.csu.mypetstore.mapper.CartItemMapper;
import org.csu.mypetstore.mapper.ProductMapper;
import org.csu.mypetstore.mapper.SalesOrderItemMapper;
import org.csu.mypetstore.mapper.SalesOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
public static final String STATUS_PAID = "PAID";
public static final String STATUS_SHIPPED = "SHIPPED";
public static final String STATUS_COMPLETED = "COMPLETED";
public static final String STATUS_CANCELLED = "CANCELLED";

private static final Map<String, String> STATUS_LABELS = Map.of(
        STATUS_PENDING_PAYMENT, "待付款",
        STATUS_PAID, "已付款",
        STATUS_SHIPPED, "已发货",
        STATUS_COMPLETED, "已完成",
        STATUS_CANCELLED, "已取消");

private static final int NAME_MAX = 50;
private static final int PHONE_MAX = 30;
private static final int ADDRESS_MAX = 255;

private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

private final CartItemMapper cartItemMapper;
private final ProductMapper productMapper;
private final SalesOrderMapper orderMapper;
private final SalesOrderItemMapper orderItemMapper;
private final CartService cartService;

public OrderService(CartItemMapper cartItemMapper,
                    ProductMapper productMapper,
                    SalesOrderMapper orderMapper,
                    SalesOrderItemMapper orderItemMapper,
                    CartService cartService) {
    this.cartItemMapper = cartItemMapper;
    this.productMapper = productMapper;
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
    this.cartService = cartService;
}

public static String statusLabel(String status) {
    return STATUS_LABELS.getOrDefault(status, status);
}

/**
 * 下单：整单一个事务。
 * 库存用「条件 UPDATE」扣减——where 里带 stock >= quantity，靠数据库行锁保证"校验 + 扣减"原子完成，
 * 并发下不会超卖；任何一步失败抛 OrderException，前面的扣减和订单写入全部回滚。
 */
@Transactional
public Long placeOrder(Long userId, Receiver receiver) {
    Receiver clean = cleanReceiver(receiver);

    List<CartItem> cartItems = cartItemMapper.selectList(
            Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId)
                    .orderByAsc(CartItem::getId));
    if (cartItems.isEmpty()) {
        throw new OrderException("购物车是空的，无法下单");
    }

    CartSnapshot snapshot = cartService.getCart(userId);
    if (snapshot.hasBlockedLine()) {
        throw new OrderException("购物车里有已下架、已售罄或超出库存的商品，请先处理");
    }

    BigDecimal total = BigDecimal.ZERO;
    List<SalesOrderItem> lines = new ArrayList<>(cartItems.size());

    for (CartItem cartItem : cartItems) {
        Product product = productMapper.selectById(cartItem.getProductId());
        if (product == null) {
            throw new OrderException("购物车里有商品已被删除，请刷新后重试");
        }
        int quantity = cartItem.getQuantity();

        int affected = productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .setSql("stock = stock - " + quantity)
                .eq(Product::getId, product.getId())
                .eq(Product::getStatus, ProductService.STATUS_ON_SALE)
                .ge(Product::getStock, quantity));
        if (affected == 0) {
            throw new OrderException("「" + product.getName() + "」库存不足，请调整数量后再下单");
        }

        BigDecimal lineAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        total = total.add(lineAmount);

        SalesOrderItem line = new SalesOrderItem();
        line.setProductId(product.getId());
        line.setSkuSnapshot(product.getSku());
        line.setProductNameSnapshot(product.getName());
        line.setCoverImageSnapshot(product.getCoverImage());
        line.setUnitPrice(product.getPrice());
        line.setQuantity(quantity);
        line.setLineAmount(lineAmount);
        lines.add(line);
    }

    if (total.compareTo(BigDecimal.ZERO) <= 0) {
        throw new OrderException("订单金额必须大于 0");
    }

    SalesOrder order = new SalesOrder();
    order.setOrderNo(nextOrderNo());
    order.setUserId(userId);
    order.setStatus(STATUS_PENDING_PAYMENT);
    order.setTotalAmount(total);
    order.setReceiverName(clean.name());
    order.setReceiverPhone(clean.phone());
    order.setReceiverAddress(clean.address());
    orderMapper.insert(order);

    for (SalesOrderItem line : lines) {
        line.setOrderId(order.getId());
        orderItemMapper.insert(line);
    }

    // 下单成功才清车；上面的异常会让这次删除也不会执行
    cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getUserId, userId));
    return order.getId();
}

public List<OrderSummary> listOrders(Long userId) {
    return orderMapper.selectList(Wrappers.<SalesOrder>lambdaQuery()
                                          .eq(SalesOrder::getUserId, userId)
                                          .orderByDesc(SalesOrder::getCreatedAt)
                                          .orderByDesc(SalesOrder::getId))
                       .stream()
                       .map(order -> new OrderSummary(order.getId(), order.getOrderNo(), order.getStatus(),
                               statusLabel(order.getStatus()), order.getTotalAmount(), order.getCreatedAt()))
                       .toList();
}

/** 只能看自己的订单：user_id 一起作为查询条件，查不到就当作不存在。 */
public Optional<SalesOrder> findOwnOrder(Long userId, Long orderId) {
    return Optional.ofNullable(orderMapper.selectOne(Wrappers.<SalesOrder>lambdaQuery()
                                                            .eq(SalesOrder::getId, orderId)
                                                            .eq(SalesOrder::getUserId, userId)));
}

public List<SalesOrderItem> findOrderItems(Long orderId) {
    return orderItemMapper.selectList(Wrappers.<SalesOrderItem>lambdaQuery()
                                              .eq(SalesOrderItem::getOrderId, orderId)
                                              .orderByAsc(SalesOrderItem::getId));
}

private Receiver cleanReceiver(Receiver receiver) {
    if (receiver == null) {
        throw new OrderException("请填写收货信息");
    }
    String name = trim(receiver.name());
    String phone = trim(receiver.phone());
    String address = trim(receiver.address());

    if (name.isEmpty() || name.length() > NAME_MAX) {
        throw new OrderException("收货人姓名不能为空，且不超过 " + NAME_MAX + " 字");
    }
    if (phone.isEmpty() || phone.length() > PHONE_MAX) {
        throw new OrderException("联系电话不能为空，且不超过 " + PHONE_MAX + " 位");
    }
    if (address.isEmpty() || address.length() > ADDRESS_MAX) {
        throw new OrderException("收货地址不能为空，且不超过 " + ADDRESS_MAX + " 字");
    }
    return new Receiver(name, phone, address);
}

private String trim(String value) {
    return value == null ? "" : value.trim();
}

private String nextOrderNo() {
    String stamp = LocalDateTime.now().format(ORDER_NO_TIME);
    int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
    return "ORD" + stamp + suffix;
}

}
