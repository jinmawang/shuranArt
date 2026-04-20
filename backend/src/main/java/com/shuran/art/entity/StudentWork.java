package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_work")
public class StudentWork {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long childId;
    private Long userId;
    private String imageUrl;
    private String description;
    private String status;
    private Boolean featured;
    private LocalDateTime createdAt;
}
