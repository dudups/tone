CREATE TABLE `project_artifact_repo` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `repo_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '制品库ID',
  `company_id` bigint(20) NOT NULL COMMENT '公司ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `repo_id` (`repo_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目关联制品库';

CREATE TABLE `project_wiki_space` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `space_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'wiki space ID',
  `company_id` bigint(20) NOT NULL COMMENT '公司ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `space_id` (`space_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目关联wiki空间';

CREATE TABLE `project_host_group` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `group_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '主机组ID',
  `company_id` bigint(20) NOT NULL COMMENT '公司ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `group_id` (`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目关联主机组资源';