-- Banner 轮播图表
USE shuran_art;

CREATE TABLE IF NOT EXISTS `banner` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `image_url` VARCHAR(512) NOT NULL,
    `description` VARCHAR(20) DEFAULT '',
    `sort_order` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
