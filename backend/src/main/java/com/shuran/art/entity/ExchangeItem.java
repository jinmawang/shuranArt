package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exchange_item")
public class ExchangeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer pointsCost;
    private Integer stock;
    private String description;
    private String image;
    private Integer needClaim;
    private Integer status;
    private LocalDateTime createdAt;
}
