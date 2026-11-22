package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 商品详情相册，对应 product_image 表（一个商品多条，封面另存 product.cover_image）。
 *
 * 待办：补字段 productId / imagePath / altText / sortOrder
 */
@TableName("product_image")
public class ProductImage {

    @TableId(type = IdType.AUTO)
    private Long id;
}
