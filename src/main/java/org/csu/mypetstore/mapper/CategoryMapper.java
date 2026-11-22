package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.entity.Category;

/**
 * 分类数据访问：继承 BaseMapper 即可，不写 XML、不写自定义 SQL。
 * 首页需要的「一次查出全部 15 条」用 selectList + 排序 Wrapper 完成。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
