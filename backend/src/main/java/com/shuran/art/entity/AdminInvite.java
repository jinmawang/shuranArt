package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("admin_invite")
public class AdminInvite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String token;
    private String inviterOpenid;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean used;
    private String usedByOpenid;
}
