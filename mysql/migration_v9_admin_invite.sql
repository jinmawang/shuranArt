-- 管理员邀请表
CREATE TABLE IF NOT EXISTS `admin_invite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `token` VARCHAR(64) UNIQUE NOT NULL,
    `inviter_openid` VARCHAR(64) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `expires_at` DATETIME NOT NULL,
    `used` TINYINT DEFAULT 0,
    `used_by_openid` VARCHAR(64),
    INDEX `idx_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理员白名单添加超级管理员标识
ALTER TABLE `admin_whitelist` ADD COLUMN IF NOT EXISTS `is_super` TINYINT DEFAULT 0;

-- 将第一个管理员设为超级管理员
UPDATE `admin_whitelist` SET `is_super` = 1 ORDER BY id LIMIT 1;
