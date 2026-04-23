package com.shuran.art.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuran.art.entity.ShareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

@Mapper
public interface ShareRecordMapper extends BaseMapper<ShareRecord> {
    @Select("SELECT COUNT(*) FROM share_record WHERE sharer_id = #{sharerId} AND created_at >= #{startTime}")
    int countTodayShares(@Param("sharerId") Long sharerId, @Param("startTime") LocalDateTime startTime);

    @Select("SELECT COUNT(*) FROM share_record WHERE sharer_id = #{sharerId} AND activity_id = #{activityId} AND confirmed = 1")
    int countConfirmedShares(@Param("sharerId") Long sharerId, @Param("activityId") Long activityId);

    @Select("SELECT COUNT(*) FROM share_record WHERE sharer_id = #{sharerId} AND activity_id = #{activityId}")
    int countTotalSharesByActivity(@Param("sharerId") Long sharerId, @Param("activityId") Long activityId);

    @Update("UPDATE share_record SET confirmed = 1, visitor_id = #{visitorId}, confirmed_at = NOW() WHERE id = #{id} AND confirmed = 0")
    int atomicConfirm(@Param("id") Long id, @Param("visitorId") Long visitorId);
}
