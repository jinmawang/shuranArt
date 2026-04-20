package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String imageUrl;
    private String description;
    private String content;
    private String shareText;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
