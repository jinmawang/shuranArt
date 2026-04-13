-- 数据库迁移脚本 v3
-- 新增功能：抽奖活动关联、每活动最大抽奖次数限制、保底机制

USE shuran_art;

-- 1. 活动表添加每人最大抽奖次数字段
SET @exist_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'shuran_art' AND table_name = 'activity' AND column_name = 'max_lottery_per_user');
SET @sql := IF(@exist_col = 0, 'ALTER TABLE `activity` ADD COLUMN `max_lottery_per_user` INT DEFAULT 10 COMMENT "每人每个活动最大抽奖次数"', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 奖品表添加等级字段
SET @exist_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'shuran_art' AND table_name = 'prize' AND column_name = 'level');
SET @sql := IF(@exist_col = 0, 'ALTER TABLE `prize` ADD COLUMN `level` INT DEFAULT 4 COMMENT "奖品等级：1=一等奖，2=二等奖，3=三等奖，4=参与奖"', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 抽奖记录表添加活动ID字段
SET @exist_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'shuran_art' AND table_name = 'lottery_record' AND column_name = 'activity_id');
SET @sql := IF(@exist_col = 0, 'ALTER TABLE `lottery_record` ADD COLUMN `activity_id` BIGINT COMMENT "活动ID" AFTER `user_id`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 抽奖记录表添加奖品等级字段
SET @exist_col := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'shuran_art' AND table_name = 'lottery_record' AND column_name = 'prize_level');
SET @sql := IF(@exist_col = 0, 'ALTER TABLE `lottery_record` ADD COLUMN `prize_level` INT DEFAULT 4 COMMENT "奖品等级" AFTER `prize_type`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 添加用户活动抽奖记录索引
SET @exist_idx := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = 'shuran_art' AND table_name = 'lottery_record' AND index_name = 'idx_user_activity');
SET @sql := IF(@exist_idx = 0, 'ALTER TABLE `lottery_record` ADD INDEX `idx_user_activity` (`user_id`, `activity_id`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 更新现有奖品的等级（根据类型和价值）
UPDATE `prize` SET `level` = 1 WHERE `type` = 'gift' AND `level` IS NULL;
UPDATE `prize` SET `level` = 2 WHERE `type` = 'experience' AND `level` IS NULL;
UPDATE `prize` SET `level` = 2 WHERE `type` = 'points' AND `value` >= 100 AND `level` IS NULL;
UPDATE `prize` SET `level` = 3 WHERE `type` = 'points' AND `value` >= 20 AND `value` < 100 AND `level` IS NULL;
UPDATE `prize` SET `level` = 4 WHERE `level` IS NULL;
