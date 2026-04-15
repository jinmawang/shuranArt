-- Migration V8: 画室日常照片、学生作品、老师微信号
-- 2026-04-15

USE shuran_art;

-- teacher 表新增微信号字段
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS wechat_id VARCHAR(64) DEFAULT NULL COMMENT '老师微信号';
