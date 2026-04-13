package com.shuran.art.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

// CRS Entity: 课程实体，映射 course 表
// 参照 Teacher.java 模式（L2 CRS-data-detail.md Section 1.1）
@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;            // 自增主键
    private String name;        // 课程名称 VARCHAR(64) NOT NULL
    private String category;    // 课程类别 VARCHAR(32) NOT NULL
    private String description; // 课程详细描述（富文本HTML）TEXT nullable
    private Integer price;      // 课程价格（单位：元），0=免费体验课 INT NOT NULL
    private String duration;    // 课程时长描述 VARCHAR(32) NOT NULL
    private String suitableFor; // 适合人群描述 VARCHAR(64) NOT NULL
    private String coverImg;    // 课程封面图URL VARCHAR(512) nullable
    private Integer sortOrder;  // 排序序号，升序排列 INT DEFAULT 0
    private Integer status;     // 状态：1=上架, 0=下架 TINYINT DEFAULT 1
    private LocalDateTime createdAt; // 创建时间 DATETIME DEFAULT CURRENT_TIMESTAMP
}
