package org.csu.mypetstore.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单列表一行。statusLabel 是展示文案，别在模板里写 switch。 */
public record OrderSummary(Long id,
                           String orderNo,
                           String status,
                           String statusLabel,
                           BigDecimal totalAmount,
                           LocalDateTime createdAt) {
}
