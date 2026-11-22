package org.csu.mypetstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 商品分类，对应 category 表（固定两级：parent_id 为空是一级）。
 * parent_key 是 STORED 生成列，故意不映射。
 */
@Getter
@Setter
@TableName("category")
public class Category {

@TableId(type = IdType.AUTO)
private Long id;

/**
 * 全局唯一编码，创建后不改（DOGS / DOGS_PETS …）
 */
private String code;

/**
 * 一级为 null，二级指向所属一级
 */
private Long parentId;

private String name;

private Integer sortOrder;

private LocalDateTime createdAt;

private LocalDateTime updatedAt;

}
