-- 数据库迁移脚本 v4
-- 新增功能：首次访问活动获得抽奖机会、抽奖次数上限改为6次

USE shuran_art;

-- 1. 创建活动访问记录表
CREATE TABLE IF NOT EXISTS `activity_visit` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `lottery_granted` INT DEFAULT 0 COMMENT '是否已发放抽奖机会：0=未发放，1=已发放',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    INDEX `idx_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动访问记录表';

-- 2. 更新活动表的默认抽奖次数为6
SET @exist_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'shuran_art' AND table_name = 'activity' AND column_name = 'max_lottery_per_user');
SET @sql := IF(@exist_col > 0, 'ALTER TABLE `activity` MODIFY COLUMN `max_lottery_per_user` INT DEFAULT 6 COMMENT "每人每个活动最大抽奖次数"', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 更新活动表的默认分享次数为6
SET @exist_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'shuran_art' AND table_name = 'activity' AND column_name = 'total_share_limit');
SET @sql := IF(@exist_col > 0, 'ALTER TABLE `activity` MODIFY COLUMN `total_share_limit` INT DEFAULT 6 COMMENT "每人每活动最大分享次数"', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
