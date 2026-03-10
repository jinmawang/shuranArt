package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.ShareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;

@Mapper
public interface ShareRecordMapper extends BaseMapper<ShareRecord> {
    @Select("SELECT COUNT(*) FROM share_record WHERE sharer_id = #{sharerId} AND created_at >= #{startTime}")
    int countTodayShares(@Param("sharerId") Long sharerId, @Param("startTime") LocalDateTime startTime);
}
