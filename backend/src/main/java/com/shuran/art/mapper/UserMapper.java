package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE user SET lottery_chances = lottery_chances - 1 WHERE id = #{userId} AND lottery_chances > 0")
    int deductLotteryChance(Long userId);
}
