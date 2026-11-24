package org.csu.mypetstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.csu.mypetstore.entity.SalesOrder;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
}
