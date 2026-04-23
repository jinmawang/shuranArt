package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.GroupBuyTeam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GroupBuyTeamMapper extends BaseMapper<GroupBuyTeam> {

    @Update("UPDATE group_buy_team SET member_count = member_count + 1 WHERE id = #{teamId} AND status = 0 AND member_count < #{groupSize}")
    int atomicIncrementMemberCount(@Param("teamId") Long teamId, @Param("groupSize") int groupSize);
}
