package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("group_buy_team")
public class GroupBuyTeam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long leaderUserId;
    private Integer status;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @TableField(exist = false)
    private List<GroupBuyMember> members;

    @TableField(exist = false)
    private String leaderNickname;

    @TableField(exist = false)
    private String leaderAvatar;
}
