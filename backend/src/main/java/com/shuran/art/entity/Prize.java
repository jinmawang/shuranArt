package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prize")
public class Prize {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer level;  // 奖品等级：1=一等奖，2=二等奖，3=三等奖，4=参与奖
    private Integer value;
    private Integer probability;
    private Integer stock;
    private String icon;
    private Integer needClaim;
    private Integer status;
    private LocalDateTime createdAt;
}
