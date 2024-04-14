ALTER TABLE `project`
    ADD COLUMN `plan_keep_days` bigint(20) NOT NULL DEFAULT '365' COMMENT '计划归档后保留天数';