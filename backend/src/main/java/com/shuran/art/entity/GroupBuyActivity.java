package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_buy_activity")
public class GroupBuyActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverImg;
    private String shareImage;
    private String shareTitle;
    private Integer groupSize;
    private String price;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Boolean started;

    @TableField(exist = false)
    private Boolean ended;
}
