ALTER TABLE `project`
  ADD COLUMN `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  ADD COLUMN `end_time` datetime DEFAULT NULL COMMENT '结束时间';