-- 轮播图增加笔记内容和分享文案字段
ALTER TABLE `banner` ADD COLUMN `content` TEXT AFTER `description`;
ALTER TABLE `banner` ADD COLUMN `share_text` VARCHAR(256) AFTER `content`;
