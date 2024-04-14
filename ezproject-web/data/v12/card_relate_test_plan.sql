CREATE TABLE `card_test_plan` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL COMMENT '卡片ID',
  `test_plan_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'test plan ID',
  `test_space_id` bigint(20) NOT NULL COMMENT 'test space ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  KEY `test_plan_id` (`test_plan_id`) USING BTREE,
  KEY `card_id` (`card_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='卡片关联测试执行计划';