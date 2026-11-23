package org.csu.mypetstore.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车视图：行明细 + 总件数 + 可结算金额。
 * totalAmount 只累加能结算的行（下架 / 售罄 / 超库存的不计入）。
 */
public record CartSnapshot(List<CartLine> lines, int itemCount, BigDecimal totalAmount) {

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public boolean hasBlockedLine() {
        return lines.stream().anyMatch(line -> !line.purchasable());
    }
}
