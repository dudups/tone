CREATE TABLE `card_token` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '卡片id',
  `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目ID',
  `token` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT 'token',
  PRIMARY KEY (`id`),
  UNIQUE KEY `card_id` (`card_id`) USING HASH,
  KEY `project_id` (`project_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='card-token';