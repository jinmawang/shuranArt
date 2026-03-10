package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
