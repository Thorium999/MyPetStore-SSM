package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表。状态与时间字段受 CHECK 约束牵制：
 * PENDING_PAYMENT 时 payment_* 必须为空；PAID/SHIPPED/COMPLETED 时必须有；CANCELLED 必须有 cancelled_at。
 */
@Getter
@Setter
@TableName("sales_order")
public class SalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String status;

    private BigDecimal totalAmount;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String paymentMethod;

    private String paymentNo;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;
}
