package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品，对应 product 表（一商品 = 一 SKU，没有多规格）。
 */
@Getter
@Setter
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 唯一销售编码，创建后不改，仅用于展示 */
    private String sku;

    /** 必须指向二级分类 */
    private Long categoryId;

    private String name;

    private String summary;

    /** 纯文本详情，模板用 th:text 转义展示 */
    private String description;

    /** 站内相对路径，如 images/products/K9-BGL-03/1.jpg */
    private String coverImage;

    private BigDecimal price;

    private Integer stock;

    /** DRAFT 下架 / ON_SALE 上架 */
    private String status;

    private Integer sortOrder;

    /** 乐观锁列，只读展示用不到，管理端写操作才用 */
    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
