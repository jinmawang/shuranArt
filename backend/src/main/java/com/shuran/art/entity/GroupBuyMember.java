package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_buy_member")
public class GroupBuyMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long userId;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private LocalDateTime joinedAt;
}
