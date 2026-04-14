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
    `status` TINYINT DEFAULT 1,
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
    `total_share_limit` INT DEFAULT 6 COMMENT '每人每个活动总分享次数限制',
    `max_lottery_per_user` INT DEFAULT 6 COMMENT '每人每个活动最大抽奖次数',
    `share_title` VARCHAR(128) COMMENT '分享标题/文案',
    `share_image` VARCHAR(512) COMMENT '分享图片',
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 奖品表
CREATE TABLE IF NOT EXISTS `prize` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `level` INT DEFAULT 4 COMMENT '奖品等级：1=一等奖，2=二等奖，3=三等奖，4=参与奖',
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
    `sharer_id` BIGINT NOT NULL COMMENT '分享者ID',
    `visitor_id` BIGINT COMMENT '访问者ID（点击后填入）',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `share_code` VARCHAR(32) UNIQUE NOT NULL COMMENT '分享码，用于追踪点击',
    `confirmed` TINYINT DEFAULT 0 COMMENT '是否已确认（被点击访问）',
    `confirmed_at` DATETIME COMMENT '确认时间',
    `lottery_granted` TINYINT DEFAULT 0 COMMENT '是否已发放抽奖机会',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sharer` (`sharer_id`, `activity_id`),
    INDEX `idx_share_code` (`share_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽奖记录表
CREATE TABLE IF NOT EXISTS `lottery_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `activity_id` BIGINT COMMENT '活动ID',
    `prize_id` BIGINT NOT NULL,
    `prize_name` VARCHAR(64),
    `prize_type` VARCHAR(20),
    `prize_level` INT DEFAULT 4 COMMENT '奖品等级',
    `prize_value` INT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `claim_code` VARCHAR(16),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` DATETIME,
    `expire_at` DATETIME,
    INDEX `idx_user` (`user_id`, `created_at`),
    INDEX `idx_user_activity` (`user_id`, `activity_id`),
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
INSERT INTO `prize` (`name`, `type`, `level`, `value`, `probability`, `stock`, `icon`, `need_claim`) VALUES
('5积分', 'points', 4, 5, 40, -1, '/images/prize-5.png', 0),
('20积分', 'points', 3, 20, 25, -1, '/images/prize-20.png', 0),
('50积分', 'points', 3, 50, 15, -1, '/images/prize-50.png', 0),
('100积分', 'points', 2, 100, 5, -1, '/images/prize-100.png', 0),
('体验课', 'experience', 2, 1, 10, 50, '/images/prize-exp.png', 1),
('画材礼包', 'gift', 1, 1, 5, 20, '/images/prize-gift.png', 1);

-- 插入测试活动
INSERT INTO `activity` (`title`, `description`, `cover_img`, `start_time`, `end_time`, `daily_share_limit`, `total_share_limit`, `share_title`, `share_image`) VALUES
('暑期班报名优惠', '分享活动，抽取丰厚奖品！', 'https://tianma.chat/images/banner-studio.jpg', '2026-06-01 00:00:00', '2026-08-31 23:59:59', 5, 6, '快来参与书染美术暑期班活动', 'https://tianma.chat/images/banner-studio.jpg');

-- 插入默认画室配置
INSERT INTO `studio_config` (`config_key`, `config_value`, `config_type`) VALUES
('studio_name', '书染美术', 'text'),
('studio_slogan', '用艺术点亮生活', 'text'),
('studio_description', '专注美术教育10年，培养学员超过2000人', 'text'),
('studio_images', '[]', 'json'),
('studio_video', '', 'text'),
('studio_address', '', 'text'),
('studio_latitude', '', 'text'),
('studio_longitude', '', 'text'),
('studio_qrcode', '', 'text'),
('studio_wechat_id', '', 'text'),
('studio_intro', '', 'text'),
('studio_intro_images', '[]', 'json'),
('share_title_template', '【分享抽奖】{activity_title}', 'text'),
('share_desc_template', '分享活动，抽取丰厚奖品！', 'text');
