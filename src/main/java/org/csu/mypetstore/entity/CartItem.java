package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 购物车条目，对应 cart_item 表。同一用户同一商品只有一条（uk_cart_user_product），
 * 重复加购是累加数量而不是插新行。
 */
@Getter
@Setter
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    /** 数据库有 CHECK 约束：1—99 */
    private Integer quantity;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
