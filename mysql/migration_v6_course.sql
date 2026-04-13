-- CRS Migration: 课程表
-- 需求来源: REQ-CRS-001, EARS-CRS-001 ~ EARS-CRS-005
-- L2 设计: CRS-data-detail.md Section 4.1

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL COMMENT '课程名称',
    `category` VARCHAR(32) NOT NULL COMMENT '课程类别（素描/水彩/油画/国画等）',
    `description` TEXT COMMENT '课程详细描述（富文本HTML）',
    `price` INT NOT NULL DEFAULT 0 COMMENT '课程价格（单位：元），0=免费体验课',
    `duration` VARCHAR(32) NOT NULL COMMENT '课程时长描述',
    `suitable_for` VARCHAR(64) NOT NULL COMMENT '适合人群描述',
    `cover_img` VARCHAR(512) COMMENT '课程封面图URL',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，升序排列',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1=上架，0=下架',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 插入示例课程数据
INSERT INTO `course` (`name`, `category`, `description`, `price`, `duration`, `suitable_for`, `cover_img`, `sort_order`, `status`) VALUES
('素描基础班', '素描', '<p>零基础入门素描课程，从握笔姿势开始，系统学习素描基本功。</p>', 2000, '3个月', '零基础学员', '/images/course-sketch.jpg', 1, 1),
('水彩提高班', '水彩', '<p>水彩进阶技法课程，学习水彩的色彩搭配与湿画法。</p>', 3000, '2个月', '有绘画基础学员', '/images/course-watercolor.jpg', 2, 1),
('油画大师班', '油画', '<p>油画创作课程，从写生到创作的完整训练体系。</p>', 5000, '4个月', '有素描基础学员', '/images/course-oil.jpg', 3, 1);
