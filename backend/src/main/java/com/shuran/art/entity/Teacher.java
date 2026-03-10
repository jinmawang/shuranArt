package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "teacher", autoResultMap = true)
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String title;
    private String intro;
    private String avatar;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> works;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
