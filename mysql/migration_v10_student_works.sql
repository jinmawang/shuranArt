-- 学员（孩子）表
CREATE TABLE IF NOT EXISTS `student_child` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `name` VARCHAR(32) NOT NULL,
    `avatar` VARCHAR(512),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学员作品表
CREATE TABLE IF NOT EXISTS `student_work` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `child_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `image_url` VARCHAR(512) NOT NULL,
    `description` VARCHAR(256),
    `status` VARCHAR(16) DEFAULT 'pending',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_child_id` (`child_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 作品墙配置
INSERT INTO `studio_config` (`config_key`, `config_value`, `config_type`) VALUES
('work_upload_interval_days', '30', 'text'),
('work_share_text', '快来看我在书染美术的作品～', 'text')
ON DUPLICATE KEY UPDATE `config_key` = `config_key`;
