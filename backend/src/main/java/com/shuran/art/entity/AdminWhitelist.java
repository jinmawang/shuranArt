package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_whitelist")
public class AdminWhitelist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String openid;
    private String name;
    private LocalDateTime createdAt;
}
