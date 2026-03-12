package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("share_record")
public class ShareRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sharerId;
    private Long visitorId;
    private Long activityId;
    private String shareCode;
    private Integer confirmed;
    private LocalDateTime confirmedAt;
    private Integer lotteryGranted;
    private LocalDateTime createdAt;
}
