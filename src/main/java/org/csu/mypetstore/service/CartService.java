package org.csu.mypetstore.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.csu.mypetstore.dto.CartLine;
import org.csu.mypetstore.dto.CartSnapshot;
import org.csu.mypetstore.entity.CartItem;
import org.csu.mypetstore.entity.Product;
import org.csu.mypetstore.mapper.CartItemMapper;
import org.csu.mypetstore.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

/** 与 cart_item 的 CHECK 约束保持一致 */
public static final int MIN_QUANTITY = 1;
public static final int MAX_QUANTITY = 99;

private final CartItemMapper cartItemMapper;
private final ProductMapper productMapper;

public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
    this.cartItemMapper = cartItemMapper;
    this.productMapper = productMapper;
}

/**
 * 取购物车。一次查出条目、一次批量查商品，避免逐行查库。
 * 每一行都标出"能不能结算"，下架 / 售罄 / 超库存都不静默吞掉。
 */
public CartSnapshot getCart(Long userId) {
    List<CartItem> items = cartItemMapper.selectList(
            Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId)
                    .orderByAsc(CartItem::getId));
    if (items.isEmpty()) {
        return new CartSnapshot(List.of(), 0, BigDecimal.ZERO);
    }

    List<Long> productIds = items.stream().map(CartItem::getProductId).distinct().toList();
    Map<Long, Product> products = productMapper.selectByIds(productIds).stream()
                                               .collect(Collectors.toMap(Product::getId, Function.identity()));

    List<CartLine> lines = new ArrayList<>(items.size());
    int itemCount = 0;
    BigDecimal total = BigDecimal.ZERO;

    for (CartItem item : items) {
        Product product = products.get(item.getProductId());
        int quantity = item.getQuantity();

        boolean onSale = product != null && ProductService.STATUS_ON_SALE.equals(product.getStatus());
        int stock = product == null || product.getStock() == null ? 0 : product.getStock();
        BigDecimal price = product == null ? BigDecimal.ZERO : product.getPrice();
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

        String issue = "";
        if (!onSale) {
            issue = "已下架";
        } else if (stock == 0) {
            issue = "已售罄";
        } else if (quantity > stock) {
            issue = "库存仅 " + stock + " 件";
        }

        lines.add(new CartLine(item.getId(), item.getProductId(),
                product == null ? "（商品已删除）" : product.getName(),
                product == null ? "" : product.getSku(),
                product == null ? "" : product.getCoverImage(),
                price, quantity, stock, onSale, subtotal, issue));

        itemCount += quantity;
        if (issue.isEmpty()) {
            total = total.add(subtotal);
        }
    }
    return new CartSnapshot(lines, itemCount, total);
}

/**
 * 加入购物车。已有同商品则累加，累加后仍受「库存上限、99」约束。
 * 返回给用户看的提示文案。
 */
public String addItem(Long userId, Long productId, int quantity) {
    Product product = productMapper.selectById(productId);
    if (product == null || !ProductService.STATUS_ON_SALE.equals(product.getStatus())) {
        return "商品不存在或已下架";
    }
    int stock = product.getStock() == null ? 0 : product.getStock();
    if (stock <= 0) {
        return "商品已售罄";
    }

    int limit = Math.min(stock, MAX_QUANTITY);
    int want = clamp(quantity, limit);

    CartItem existing = cartItemMapper.selectOne(
            Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getProductId, productId));

    if (existing == null) {
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(want);
        item.setVersion(0L);
        cartItemMapper.insert(item);
        return "已加入购物车";
    }

    int merged = Math.min(existing.getQuantity() + want, limit);
    if (merged == existing.getQuantity()) {
        return "购物车里已有 " + merged + " 件，达到可购上限";
    }
    existing.setQuantity(merged);
    cartItemMapper.updateById(existing);
    return "已加入购物车，现有 " + merged + " 件";
}

/** 改数量：越界自动夹到 1—min(库存, 99)，并如实告诉用户。 */
public String updateQuantity(Long userId, Long itemId, int quantity) {
    CartItem item = findOwnedItem(userId, itemId);
    if (item == null) {
        return "购物车条目不存在";
    }
    Product product = productMapper.selectById(item.getProductId());
    int stock = product == null || product.getStock() == null ? 0 : product.getStock();
    if (stock <= 0) {
        return "该商品已售罄，无法调整数量";
    }

    int limit = Math.min(stock, MAX_QUANTITY);
    int applied = clamp(quantity, limit);
    item.setQuantity(applied);
    cartItemMapper.updateById(item);

    return applied == quantity ? "数量已更新" : "库存只有 " + limit + " 件，数量已调整";
}

/** 删除一行。只有本人能删（user_id 一起作为条件）。 */
public boolean removeItem(Long userId, Long itemId) {
    return cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                                         .eq(CartItem::getId, itemId)
                                         .eq(CartItem::getUserId, userId)) > 0;
}

/** 头部徽标用：所有行的数量之和。 */
public int countItems(Long userId) {
    return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                                             .eq(CartItem::getUserId, userId))
                         .stream()
                         .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                         .sum();
}

private CartItem findOwnedItem(Long userId, Long itemId) {
    return cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                                            .eq(CartItem::getId, itemId)
                                            .eq(CartItem::getUserId, userId));
}

private int clamp(int quantity, int limit) {
    return Math.min(Math.max(quantity, MIN_QUANTITY), limit);
}

}
