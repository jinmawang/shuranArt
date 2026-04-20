-- student_work 增加加精标记
ALTER TABLE `student_work` ADD COLUMN `featured` TINYINT DEFAULT 0 AFTER `status`;
