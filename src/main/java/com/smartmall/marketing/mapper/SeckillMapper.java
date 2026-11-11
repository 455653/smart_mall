package com.smartmall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartmall.marketing.domain.Seckill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 秒杀活动持久层。 */
@Mapper
public interface SeckillMapper extends BaseMapper<Seckill> {

    /** 条件占用秒杀名额，返回 0 表示售罄，SQL 见 resources/mapper/marketing/SeckillMapper.xml。 */
    int occupy(@Param("seckillId") Long seckillId, @Param("num") int num);
}
