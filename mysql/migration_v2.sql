-- 数据库迁移脚本 v2
-- 新增功能：分享确认机制、活动分享配置、画室地址配置

USE shuran_art;

-- 1. 活动表添加分享配置字段
ALTER TABLE `activity`
ADD COLUMN IF NOT EXISTS `total_share_limit` INT DEFAULT 5 COMMENT '每人每个活动总分享次数限制' AFTER `daily_share_limit`,
ADD COLUMN IF NOT EXISTS `share_title` VARCHAR(128) COMMENT '分享标题/文案' AFTER `total_share_limit`,
ADD COLUMN IF NOT EXISTS `share_image` VARCHAR(512) COMMENT '分享图片' AFTER `share_title`;

-- 2. 分享记录表添加确认机制字段
ALTER TABLE `share_record`
ADD COLUMN IF NOT EXISTS `share_code` VARCHAR(32) COMMENT '分享码，用于追踪点击' AFTER `activity_id`,
ADD COLUMN IF NOT EXISTS `confirmed` TINYINT DEFAULT 0 COMMENT '是否已确认（被点击访问）' AFTER `share_code`,
ADD COLUMN IF NOT EXISTS `confirmed_at` DATETIME COMMENT '确认时间' AFTER `confirmed`;

-- 修改 visitor_id 允许为空（分享时还不知道谁会点击）
ALTER TABLE `share_record` MODIFY COLUMN `visitor_id` BIGINT NULL;

-- 添加 share_code 索引
-- 先检查并删除旧的唯一索引（如果存在）
SET @exist_uk := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = 'shuran_art' AND table_name = 'share_record' AND index_name = 'uk_share');
SET @sql_drop := IF(@exist_uk > 0, 'ALTER TABLE `share_record` DROP INDEX `uk_share`', 'SELECT 1');
PREPARE stmt FROM @sql_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 share_code 索引（如果不存在）
SET @exist_idx := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = 'shuran_art' AND table_name = 'share_record' AND index_name = 'idx_share_code');
SET @sql_idx := IF(@exist_idx = 0, 'ALTER TABLE `share_record` ADD INDEX `idx_share_code` (`share_code`)', 'SELECT 1');
PREPARE stmt FROM @sql_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加新的画室配置项
INSERT IGNORE INTO `studio_config` (`config_key`, `config_value`, `config_type`) VALUES
('studio_address', '', 'text'),
('studio_latitude', '', 'text'),
('studio_longitude', '', 'text'),
('studio_qrcode', '', 'text'),
('studio_intro', '', 'text'),
('studio_intro_images', '[]', 'json');

-- 为现有分享记录生成 share_code（如果为空）
UPDATE `share_record` SET `share_code` = CONCAT('S', id, '_', UNIX_TIMESTAMP()) WHERE `share_code` IS NULL OR `share_code` = '';
