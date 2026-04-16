package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.Prize;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrizeMapper extends BaseMapper<Prize> {

    @Update("UPDATE prize SET stock = stock - 1 WHERE id = #{prizeId} AND stock > 0")
    int deductStock(Long prizeId);
}
