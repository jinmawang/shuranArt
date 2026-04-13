-- H2-compatible schema for integration tests
-- Adapted from mysql/init.sql + migration_v6_course.sql

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) NOT NULL,
    `nick_name` VARCHAR(64),
    `avatar_url` VARCHAR(512),
    `phone` VARCHAR(20),
    `points` INT DEFAULT 0,
    `lottery_chances` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `teacher` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(32) NOT NULL,
    `title` VARCHAR(32),
    `intro` TEXT,
    `avatar` VARCHAR(512),
    `works` TEXT,
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `activity` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `title` VARCHAR(128) NOT NULL,
    `description` TEXT,
    `cover_img` VARCHAR(512),
    `start_time` TIMESTAMP,
    `end_time` TIMESTAMP,
    `daily_share_limit` INT DEFAULT 5,
    `total_share_limit` INT DEFAULT 5,
    `max_lottery_per_user` INT DEFAULT 10,
    `share_title` VARCHAR(128),
    `share_image` VARCHAR(512),
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `prize` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `level` INT DEFAULT 4,
    `value` INT DEFAULT 0,
    `probability` INT DEFAULT 0,
    `stock` INT DEFAULT -1,
    `icon` VARCHAR(512),
    `need_claim` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `share_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `sharer_id` BIGINT NOT NULL,
    `visitor_id` BIGINT,
    `activity_id` BIGINT NOT NULL,
    `share_code` VARCHAR(32) NOT NULL,
    `confirmed` TINYINT DEFAULT 0,
    `confirmed_at` TIMESTAMP,
    `lottery_granted` TINYINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `lottery_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `activity_id` BIGINT,
    `prize_id` BIGINT NOT NULL,
    `prize_name` VARCHAR(64),
    `prize_type` VARCHAR(20),
    `prize_level` INT DEFAULT 4,
    `prize_value` INT,
    `status` VARCHAR(20) DEFAULT 'pending',
    `claim_code` VARCHAR(16),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` TIMESTAMP,
    `expire_at` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `exchange_item` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `points_cost` INT NOT NULL,
    `stock` INT DEFAULT 0,
    `description` VARCHAR(256),
    `image` VARCHAR(512),
    `need_claim` TINYINT DEFAULT 1,
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `exchange_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `item_id` BIGINT NOT NULL,
    `item_name` VARCHAR(64),
    `points_cost` INT,
    `claim_code` VARCHAR(16),
    `status` VARCHAR(20) DEFAULT 'pending',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `claimed_at` TIMESTAMP,
    `expire_at` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `admin_whitelist` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `openid` VARCHAR(64) NOT NULL,
    `name` VARCHAR(32),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `studio_config` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `config_key` VARCHAR(64) NOT NULL,
    `config_value` TEXT,
    `config_type` VARCHAR(20) DEFAULT 'text',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL,
    `category` VARCHAR(32) NOT NULL,
    `description` TEXT,
    `price` INT NOT NULL DEFAULT 0,
    `duration` VARCHAR(32) NOT NULL,
    `suitable_for` VARCHAR(64) NOT NULL,
    `cover_img` VARCHAR(512),
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `activity_visit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT,
    `activity_id` BIGINT NOT NULL,
    `lottery_granted` TINYINT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
