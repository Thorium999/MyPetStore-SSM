package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 商品分类，对应 category 表（固定两级：parent_id 为空是一级）。
 *
 * 待办：
 * 1. 按库表补字段：id / code / parentId / name / sortOrder / createdAt / updatedAt
 * 2. parent_key 是 STORED 生成列，**不要映射**（映射了插入会报错）
 * 3. 不加 children 之类的关联字段，层级关系由 CategoryNavigation 承载
 */
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;
}
