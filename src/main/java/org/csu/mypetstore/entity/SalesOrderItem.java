package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单明细：下单那一刻的商品快照，之后商品改名改价都不影响历史订单。
 */
@Getter
@Setter
@TableName("sales_order_item")
public class SalesOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    private String skuSnapshot;

    private String productNameSnapshot;

    private String coverImageSnapshot;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal lineAmount;
}
