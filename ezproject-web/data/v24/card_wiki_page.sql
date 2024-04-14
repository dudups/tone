
CREATE TABLE `card_wiki_page_rel` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL COMMENT '卡片ID',
  `page_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'page ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  `wiki_space_id` bigint(20) NOT NULL COMMENT 'wiki space Id',
  PRIMARY KEY (`id`),
  KEY `page_id` (`page_id`) USING BTREE,
  KEY `card_id` (`card_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='卡片关联wiki page';

CREATE TABLE `project_doc_space` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `space_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'doc space ID',
  `company_id` bigint(20) NOT NULL COMMENT '公司ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `space_id` (`space_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目关联doc空间';

CREATE TABLE `card_doc_rel` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL COMMENT '卡片ID',
  `doc_id` bigint(20) NOT NULL DEFAULT '0' COMMENT 'doc ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  `doc_space_id` bigint(20) NOT NULL COMMENT 'doc space Id',
  PRIMARY KEY (`id`),
  KEY `doc_id` (`doc_id`) USING BTREE,
  KEY `card_id` (`card_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='卡片关联doc';