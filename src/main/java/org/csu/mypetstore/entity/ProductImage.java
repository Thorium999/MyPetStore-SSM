package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 商品详情相册，对应 product_image 表。
 * 一个商品多条，按 sort_order、id 升序取；封面另存 product.cover_image。
 */
@Getter
@Setter
@TableName("product_image")
public class ProductImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String imagePath;

    private String altText;

    private Integer sortOrder;
}
