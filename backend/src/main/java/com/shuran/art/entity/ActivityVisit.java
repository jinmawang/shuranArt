package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_visit")
public class ActivityVisit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private Integer lotteryGranted;  // 是否已发放抽奖机会
    private LocalDateTime createdAt;
}
