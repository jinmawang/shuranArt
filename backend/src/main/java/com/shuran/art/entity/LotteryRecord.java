package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("lottery_record")
public class LotteryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long prizeId;
    private String prizeName;
    private String prizeType;
    private Integer prizeValue;
    private String status;
    private String claimCode;
    private LocalDateTime createdAt;
    private LocalDateTime claimedAt;
    private LocalDateTime expireAt;
}
