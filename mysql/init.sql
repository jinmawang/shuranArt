-- 画室小程序数据库初始化脚本
CREATE DATABASE IF NOT EXISTS shuran_art DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shuran_art;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) UNIQUE NOT NULL,
    `nick_name` VARCHAR(64),
    `avatar_url` VARCHAR(512),
    `phone` VARCHAR(20),
    `points` INT DEFAULT 0,
    `lottery_chances` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 老师表
CREATE TABLE IF NOT EXISTS `teacher` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,
    `title` VARCHAR(32),
    `intro` TEXT,
    `avatar` VARCHAR(512),
    `works` JSON,
    `sort_order` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 活动表
CREATE TABLE IF NOT EXISTS `activity` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(128) NOT NULL,
    `description` TEXT,
    `cover_img` VARCHAR(512),
    `start_time` DATETIME,
    `end_time` DATETIME,
    `daily_share_limit` INT DEFAULT 5,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 奖品表
CREATE TABLE IF NOT EXISTS `prize` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `value` INT DEFAULT 0,
    `probability` INT DEFAULT 0,
    `stock` INT DEFAULT -1,
    `icon` VARCHAR(512),
    `need_claim` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分享记录表
CREATE TABLE IF NOT EXISTS `share_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sharer_id` BIGINT NOT NULL,
    `visitor_id` BIGINT NOT NULL,
    `activity_id` BIGINT NOT NULL,
    `lottery_granted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_share` (`sharer_id`, `visitor_id`, `activity_id`),
    INDEX `idx_sharer` (`sharer_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽奖记录表
CREATE TABLE IF NOT EXISTS `lottery_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `prize_id` BIGINT NOT NULL,
    `prize_name` VARCHAR(64),
    `prize_type` VARCHAR(20),
    `prize_value` INT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `claim_code` VARCHAR(16),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` DATETIME,
    `expire_at` DATETIME,
    INDEX `idx_user` (`user_id`, `created_at`),
    INDEX `idx_claim_code` (`claim_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兑换商品表
CREATE TABLE IF NOT EXISTS `exchange_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `points_cost` INT NOT NULL,
    `stock` INT DEFAULT 0,
    `description` VARCHAR(256),
    `image` VARCHAR(512),
    `need_claim` TINYINT DEFAULT 1,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兑换记录表
CREATE TABLE IF NOT EXISTS `exchange_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `item_name` VARCHAR(64),
    `points_cost` INT,
    `claim_code` VARCHAR(16),
    `status` VARCHAR(20) DEFAULT 'pending',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` DATETIME,
    `expire_at` DATETIME,
    INDEX `idx_user` (`user_id`, `created_at`),
    INDEX `idx_claim_code` (`claim_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员白名单表
CREATE TABLE IF NOT EXISTS `admin_whitelist` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) UNIQUE NOT NULL,
    `name` VARCHAR(32),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 画室配置表
CREATE TABLE IF NOT EXISTS `studio_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(64) UNIQUE NOT NULL,
    `config_value` TEXT,
    `config_type` VARCHAR(20) DEFAULT 'text',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认奖品数据
INSERT INTO `prize` (`name`, `type`, `value`, `probability`, `stock`, `icon`, `need_claim`) VALUES
('5积分', 'points', 5, 40, -1, '/images/prize-5.png', 0),
('20积分', 'points', 20, 25, -1, '/images/prize-20.png', 0),
('50积分', 'points', 50, 15, -1, '/images/prize-50.png', 0),
('100积分', 'points', 100, 5, -1, '/images/prize-100.png', 0),
('体验课', 'experience', 1, 10, 50, '/images/prize-exp.png', 1),
('画材礼包', 'gift', 1, 5, 20, '/images/prize-gift.png', 1);

-- 插入测试活动
INSERT INTO `activity` (`title`, `description`, `cover_img`, `start_time`, `end_time`, `daily_share_limit`) VALUES
('暑期班报名优惠', '分享活动，抽取丰厚奖品！', '/images/activity-summer.jpg', '2026-06-01 00:00:00', '2026-08-31 23:59:59', 5);

-- 插入默认画室配置
INSERT INTO `studio_config` (`config_key`, `config_value`, `config_type`) VALUES
('studio_name', '舒然画室', 'text'),
('studio_slogan', '用艺术点亮生活', 'text'),
('studio_description', '专注美术教育10年，培养学员超过2000人', 'text'),
('studio_images', '[]', 'json'),
('studio_video', '', 'text'),
('share_title_template', '【分享抽奖】{activity_title}', 'text'),
('share_desc_template', '分享活动，抽取丰厚奖品！', 'text');
