ALTER TABLE `project_chart`
    ADD COLUMN `group_id` bigint(20) NOT NULL COMMENT '分组',
    ADD COLUMN `seq_num` int(10) NOT NULL DEFAULT '0' COMMENT '顺序编号',
    ADD COLUMN `type` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '类型';

ALTER TABLE `project_chart` ADD KEY `group_id` (`group_id`) USING BTREE;

ALTER TABLE project_chart MODIFY COLUMN `title` varchar(32) COLLATE utf8mb4_bin NOT NULL;


CREATE TABLE `project_chart_group` (
    `id` bigint(20) NOT NULL COMMENT '组id',
    `title` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '标题',
    `type` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '类型',
    `project_id` bigint(20) NOT NULL COMMENT '所属项目ID',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
    `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
    `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `project_id` (`project_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='报表组';