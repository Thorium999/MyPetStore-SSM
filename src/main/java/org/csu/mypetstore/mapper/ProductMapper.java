package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.entity.Product;

/**
 * 商品数据访问。
 * 列表页的分页 / 条件筛选、详情页的按 id 查询，全部用 BaseMapper + Wrapper 或 Page 完成，
 * 本期不写 XML。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
