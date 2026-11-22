package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.entity.ProductImage;

/**
 * 相册数据访问：详情页按 productId 取，按 sort_order、id 升序。
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
}
