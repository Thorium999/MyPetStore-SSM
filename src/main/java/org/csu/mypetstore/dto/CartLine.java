package org.csu.mypetstore.dto;

import java.math.BigDecimal;

/**
 * 购物车里的一行：条目 + 商品快照 + 能否结算。
 * issues 是给模板直接展示的提示文案，空字符串表示这行正常。
 */
public record CartLine(Long itemId,
                       Long productId,
                       String name,
                       String sku,
                       String coverImage,
                       BigDecimal price,
                       int quantity,
                       int stock,
                       boolean onSale,
                       BigDecimal subtotal,
                       String issue) {

    public boolean purchasable() {
        return issue.isEmpty();
    }
}
