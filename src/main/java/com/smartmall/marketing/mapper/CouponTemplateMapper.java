package com.smartmall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.marketing.domain.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 优惠券模板持久层。 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /** 条件自增已领数量，返回 0 表示已领完，SQL 见 resources/mapper/marketing/CouponTemplateMapper.xml。 */
    int increaseReceived(@Param("id") Long id);
}
