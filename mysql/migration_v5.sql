-- v5: 添加老师微信号配置
USE shuran_art;

INSERT INTO `studio_config` (`config_key`, `config_value`, `config_type`)
SELECT 'studio_wechat_id', '', 'text'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `studio_config` WHERE `config_key` = 'studio_wechat_id');
