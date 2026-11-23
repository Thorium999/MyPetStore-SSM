package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.entity.CartItem;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
