package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_child")
public class StudentChild {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String avatar;
    private LocalDateTime createdAt;
}
