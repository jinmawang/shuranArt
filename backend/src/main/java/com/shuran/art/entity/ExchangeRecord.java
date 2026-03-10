package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exchange_record")
public class ExchangeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private Integer pointsCost;
    private String claimCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime claimedAt;
    private LocalDateTime expireAt;
}
