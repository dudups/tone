CREATE TABLE `project_k8s_group` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `k8s_group_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'k8s 集群ID',
  `company_id` bigint(20) NOT NULL COMMENT '公司ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `k8s_group_id` (`k8s_group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目关联k8s集群';
