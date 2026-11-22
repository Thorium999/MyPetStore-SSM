package org.csu.mypetstore.dto;

import org.csu.mypetstore.entity.Category;

import java.util.List;

/**
 * 首页分类导航的传输对象：一个一级分类 + 它下面的二级分类。
 *
 * 为什么需要它：category 表是 15 条平铺记录（5 个一级 + 10 个二级），
 * 页面要的是「一级带子级」的树，用这个 record 装起来，不给表加冗余字段。
 *
 * 与 UserView 同样放在 dto 包；record 是不可变数据载体，不写 setter。
 */
public record CategoryNavigation(Category category, List<Category> children) {
}
