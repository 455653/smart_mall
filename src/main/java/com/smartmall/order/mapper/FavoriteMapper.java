package com.smartmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.order.domain.Favorite;
import org.apache.ibatis.annotations.Mapper;

/** 商品收藏持久层。 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
