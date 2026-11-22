package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 商品，对应 product 表（一商品 = 一 SKU，没有多规格）。
 *
 * 待办：
 * 1. 按库表补字段：sku / categoryId / name / summary / description / coverImage /
 *    price(BigDecimal) / stock(Integer) / status / sortOrder / version / createdAt / updatedAt
 * 2. coverImage 存的是站内相对路径（如 images/products/K9-BGL-03/1.jpg），模板里拼前导斜杠
 * 3. version 是乐观锁列，本期只读展示用不到，写操作（管理端）才需要
 */
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;
}
