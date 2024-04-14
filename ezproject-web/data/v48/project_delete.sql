ALTER TABLE `project`
    ADD COLUMN `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '活跃或已归档';