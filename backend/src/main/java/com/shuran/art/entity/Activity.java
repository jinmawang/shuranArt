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
    private Integer status;
    private LocalDateTime createdAt;
}
