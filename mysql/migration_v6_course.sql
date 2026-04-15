-- 课程表

CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL COMMENT '课程名称',
    `category` VARCHAR(32) NOT NULL COMMENT '课程分类（素描/水彩/油画/国画/手工等）',
    `description` TEXT COMMENT '课程简介',
    `price` INT DEFAULT 0 COMMENT '课程价格（保留字段）',
    `duration` VARCHAR(32) DEFAULT NULL COMMENT '课程时长（保留字段）',
    `suitable_for` VARCHAR(64) NOT NULL COMMENT '适合年龄',
    `cover_img` VARCHAR(512) COMMENT '课程封面图URL',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，升序排列',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1=上架，0=下架',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';
