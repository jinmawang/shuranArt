-- student_child 增加审核状态和申请理由
ALTER TABLE `student_child` ADD COLUMN `status` VARCHAR(16) DEFAULT 'pending' AFTER `avatar`;
ALTER TABLE `student_child` ADD COLUMN `reason` VARCHAR(256) AFTER `status`;

-- 已有数据设为已通过
UPDATE `student_child` SET `status` = 'approved' WHERE `status` = 'pending';
