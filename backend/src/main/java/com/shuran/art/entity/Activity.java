package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverImg;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer dailyShareLimit;
    private Integer totalShareLimit;
    private Integer maxLotteryPerUser;  // 每人每活动最大抽奖次数
    private String shareTitle;
    private String shareImage;
    private Integer status;
    private LocalDateTime createdAt;

    // 是否已开始（非数据库字段）
    @TableField(exist = false)
    private Boolean started;

    // 是否已结束（非数据库字段）
    @TableField(exist = false)
    private Boolean ended;
}
