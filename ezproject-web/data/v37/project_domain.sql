CREATE TABLE `project_domain` (
  `id` bigint(20) NOT NULL COMMENT '项目id',
  `max_seq_num` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目下卡片最大编号',
  `max_rank` varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '优先级排序地位，字典序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='project domain';

INSERT INTO
  `project_domain`(`id`, `max_seq_num`, `max_rank`)
  SELECT `id`, `max_seq_num`, `max_rank` from `project`;