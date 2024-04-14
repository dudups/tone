/*
 Navicat Premium Data Transfer

 Source Server         : ezproject_dev
 Source Server Type    : MySQL
 Source Server Version : 50719
 Source Host           : 10.1.0.15:3306
 Source Schema         : ezproject_dev

 Target Server Type    : MySQL
 Target Server Version : 50719
 File Encoding         : 65001

 Date: 04/11/2021 18:23:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for attachment
-- ----------------------------
CREATE TABLE `attachment` (
  `id` bigint(20) NOT NULL COMMENT '附件id',
  `file_name` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '附件文件名',
  `storage_path` varchar(256) CHARACTER SET utf8 NOT NULL COMMENT '附件存储路径',
  `description` varchar(256) COLLATE utf8mb4_bin NOT NULL COMMENT '描述',
  `upload_time` datetime NOT NULL COMMENT '上传时间',
  `upload_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '上传人',
  `content_type` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '文件类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='附件';

-- ----------------------------
-- Table structure for card
-- ----------------------------
CREATE TABLE `card` (
  `id` bigint(20) NOT NULL COMMENT '卡片id',
  `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属项目ID',
  `project_key` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '所属项目标识',
  `seq_num` bigint(20) NOT NULL COMMENT '卡片编号',
  `plan_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '绑定计划ID',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父卡片ID',
  `ancestor_id` bigint(20) NOT NULL COMMENT '祖先卡片ID',
  `rank` varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '优先级排序地位，字典序',
  `company_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '公司',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除',
  `max_comment_seq_num` bigint(20) NOT NULL DEFAULT '0' COMMENT '评论最大楼层',
  `story_map_node_id` bigint(20) NOT NULL COMMENT '故事地图分类节点ID',
  `latest_event_id` bigint(20) NOT NULL COMMENT '最近卡片变更事件ID',
  PRIMARY KEY (`id`),
  KEY `project_id_rank` (`project_id`,`rank`) USING BTREE,
  KEY `parent_id` (`parent_id`) USING BTREE,
  KEY `ancestor_id` (`ancestor_id`) USING BTREE,
  KEY `company_id_project_key_seq_num` (`company_id`,`project_key`,`seq_num`) USING BTREE,
  KEY `story_map_node_id` (`story_map_node_id`) USING BTREE,
  KEY `plan_id` (`plan_id`) USING BTREE,
  KEY `project_id_plan_id` (`project_id`,`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='卡片';

-- ----------------------------
-- Table structure for card_attachment_rel
-- ----------------------------
CREATE TABLE `card_attachment_rel` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL COMMENT '卡片id',
  `attachment_id` bigint(20) NOT NULL COMMENT '附件id',
  PRIMARY KEY (`id`),
  KEY `card_id` (`card_id`) USING BTREE,
  KEY `attachment_id` (`attachment_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='卡片附件关系';

-- ----------------------------
-- Table structure for card_query_view
-- ----------------------------
CREATE TABLE `card_query_view` (
  `id` bigint(20) NOT NULL COMMENT '查询视图id',
  `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目ID',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '视图名',
  `type` varchar(32) CHARACTER SET utf8 NOT NULL COMMENT '视图类型',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  `rank` bigint(20) NOT NULL COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `project_id_type_create_user` (`project_id`,`type`,`create_user`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='查询视图';

-- ----------------------------
-- Table structure for card_relate_rel
-- ----------------------------
CREATE TABLE `card_relate_rel` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL COMMENT '卡片id',
  `related_card_id` bigint(20) NOT NULL COMMENT '关联id',
  PRIMARY KEY (`id`),
  KEY `card_id` (`card_id`) USING BTREE,
  KEY `related_card_id` (`related_card_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='卡片关联关系';

-- ----------------------------
-- Table structure for card_token
-- ----------------------------
CREATE TABLE `card_token` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `card_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '卡片id',
  `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目ID',
  `token` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT 'token',
  PRIMARY KEY (`id`),
  UNIQUE KEY `card_id` (`card_id`) USING HASH,
  KEY `project_id` (`project_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='card-token';

-- ----------------------------
-- Table structure for plan
-- ----------------------------
CREATE TABLE `plan` (
  `id` bigint(20) NOT NULL COMMENT '计划id',
  `name` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '计划名',
  `project_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属项目ID',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父计划ID',
  `ancestor_id` bigint(20) NOT NULL COMMENT '祖先计划ID',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '活跃或已归档',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  `deliver_line_rank` varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '交付线-含义同卡片排序字段',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `project_id_deliver_line_rank` (`project_id`,`deliver_line_rank`) USING BTREE,
  KEY `parent_id_active` (`parent_id`,`is_active`) USING BTREE,
  KEY `ancestor_id_active` (`ancestor_id`,`is_active`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='计划';

-- ----------------------------
-- Table structure for plan_goal
-- ----------------------------
CREATE TABLE `plan_goal` (
  `id` bigint(20) NOT NULL COMMENT '卡片id',
  `plan_id` bigint(20) NOT NULL COMMENT '绑定计划ID',
  `owner` varchar(128) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '负责人',
  `status` varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '状态',
  `description` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '目标描述',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  PRIMARY KEY (`id`),
  KEY `plan_id` (`plan_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='计划的里程碑目标';

-- ----------------------------
-- Table structure for project
-- ----------------------------
CREATE TABLE `project` (
  `id` bigint(20) NOT NULL COMMENT '项目id',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '项目名',
  `company_id` bigint(20) NOT NULL COMMENT '公司',
  `description` varchar(200) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '项目描述',
  `key` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '项目标识',
  `max_seq_num` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目下卡片最大编号',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  `is_private` bit(1) NOT NULL DEFAULT b'1' COMMENT '企业内私有/公开',
  `max_rank` varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '优先级排序地位，字典序',
  `keep_days` bigint(20) NOT NULL DEFAULT '180' COMMENT '卡片逻辑删除后保留天数',
  `plan_keep_days` bigint(20) NOT NULL DEFAULT '365' COMMENT '计划归档后保留天数',
  `top_score` bigint(20) NOT NULL DEFAULT '0' COMMENT '置顶排序值（加入置顶时的时间戳， 0表示不置顶，值越大，排序越靠前。',
  `is_strict` bit(1) NOT NULL DEFAULT b'0' COMMENT '严格模式',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '活跃或已归档',
  PRIMARY KEY (`id`),
  KEY `company_id_project_key` (`company_id`,`key`) USING BTREE,
  KEY `company_id_project_name` (`company_id`,`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='project';

CREATE TABLE `project_domain` (
  `id` bigint(20) NOT NULL COMMENT '项目id',
  `max_seq_num` bigint(20) NOT NULL DEFAULT '0' COMMENT '项目下卡片最大编号',
  `max_rank` varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '优先级排序地位，字典序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='project domain';

-- ----------------------------
-- Table structure for project_card_template
-- ----------------------------
CREATE TABLE `project_card_template` (
  `id` bigint(20) NOT NULL COMMENT '项目卡片模版id',
  `project_id` bigint(20) NOT NULL COMMENT '项目id',
  `card_type` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '卡片类型',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  PRIMARY KEY (`id`),
  KEY `project_id_card_type` (`project_id`,`card_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目卡片模版';

-- ----------------------------
-- Table structure for project_chart
-- ----------------------------
CREATE TABLE `project_chart` (
  `id` bigint(20) NOT NULL COMMENT '报表id',
  `title` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '标题',
  `type` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '类型',
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID',
  `group_id` bigint(20) NOT NULL COMMENT '分组',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  `seq_num` int(10) NOT NULL DEFAULT '0' COMMENT '顺序编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `group_id` (`group_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='报表';

-- ----------------------------
-- Table structure for project_chart_group
-- ----------------------------
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

-- ----------------------------
-- Table structure for project_favourite
-- ----------------------------
CREATE TABLE `project_favourite` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '人',
  `time` datetime NOT NULL COMMENT '时间',
  `company_id` bigint(20) NOT NULL COMMENT '公司',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  PRIMARY KEY (`id`),
  KEY `user` (`user`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='收藏';

-- ----------------------------
-- Table structure for project_member
-- ----------------------------
CREATE TABLE `project_member` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `user_type` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '用户类型',
  `user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '成员用户名',
  `role` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '成员角色',
  `company_id` bigint(40) NOT NULL COMMENT '公司ID',
  `role_source` varchar(40) COLLATE utf8mb4_bin NOT NULL DEFAULT 'SYS' COMMENT '角色来源',
  `role_type` varchar(40) COLLATE utf8mb4_bin NOT NULL DEFAULT 'MEMBER' COMMENT '角色类型',
  PRIMARY KEY (`id`),
  KEY `project_id_user` (`project_id`,`user`) USING BTREE,
  KEY `user` (`user`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目成员';

-- ----------------------------
-- Table structure for project_repo
-- ----------------------------
CREATE TABLE `project_repo` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '项目ID',
  `company_id` bigint(20) NOT NULL COMMENT '公司ID',
  `create_user` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '添加人',
  `create_time` datetime NOT NULL COMMENT '添加时间',
  `repo_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '代码库ID',
  PRIMARY KEY (`id`),
  KEY `project_id` (`project_id`) USING BTREE,
  KEY `repo_id` (`repo_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目关联代码库';

-- ----------------------------
-- Table structure for project_template
-- ----------------------------
CREATE TABLE `project_template` (
  `id` bigint(20) NOT NULL COMMENT '项目模版id',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL DEFAULT '0' COMMENT '项目模版名',
  `company_id` bigint(20) NOT NULL COMMENT '公司',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  `enable` bit(1) NOT NULL DEFAULT b'1' COMMENT '启用',
  `source` varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '来源于系统/自定义',
  PRIMARY KEY (`id`),
  KEY `company_id` (`company_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='项目模版';

-- ----------------------------
-- Table structure for story_map
-- ----------------------------
CREATE TABLE `story_map` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `project_id` (`project_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='故事地图';

-- ----------------------------
-- Table structure for story_map_node
-- ----------------------------
CREATE TABLE `story_map_node` (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `project_id` bigint(20) NOT NULL COMMENT '所属项目ID',
  `name` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `story_map_id` bigint(20) NOT NULL COMMENT '故事地图ID',
  `parent_id` bigint(20) NOT NULL COMMENT '父节点ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建人',
  `last_modify_time` datetime NOT NULL COMMENT '最近修改时间',
  `last_modify_user` varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '最近修改人',
  `seq_index` bigint(20) NOT NULL COMMENT '节点顺序下标',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `story_map_id` (`story_map_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='故事地图节点';

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

create table portfolio
(
    `id`          bigint(20)   NOT NULL COMMENT '主键',
    `name`        varchar(32)  NOT NULL COMMENT '项目集名称',
    `company_id`  bigint(20)   NOT NULL COMMENT '公司ID',
    `start_date`  datetime     NULL COMMENT '开始时间',
    `end_date`    datetime     NULL COMMENT '结束时间',
    `parent_id`   bigint(20)   not null default 0 comment '父项目集ID',
    `ancestor_id` bigint(20)   not null default 0 comment '祖先项目集ID',
    `path`        varchar(255) not null comment '项目集路径',
    constraint project_collection_pk
        primary key (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='项目集';

CREATE TABLE `portfolio_member`
(
    `id`           bigint(20)                       NOT NULL COMMENT 'id',
    `portfolio_id` bigint(20)                       NOT NULL COMMENT '项目集ID',
    `user_type`    varchar(40) COLLATE utf8mb4_bin  NOT NULL COMMENT '用户类型',
    `user`         varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '成员用户名',
    `role`         varchar(40) COLLATE utf8mb4_bin  NOT NULL COMMENT '成员角色',
    `company_id`   bigint(20)                       NOT NULL COMMENT '公司ID',
    `role_source`  varchar(40) COLLATE utf8mb4_bin  NOT NULL DEFAULT 'SYS' COMMENT '角色来源',
    `role_type`    varchar(40) COLLATE utf8mb4_bin  NOT NULL DEFAULT 'MEMBER' COMMENT '角色类型',
    PRIMARY KEY (`id`),
    KEY `portfolio_id_user` (`portfolio_id`, `user`) USING BTREE,
    KEY `user` (`user`) USING BTREE,
    KEY `project_member_company_id_role_index` (`company_id`, `role`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='项目集成员';

CREATE TABLE `rel_portfolio_project`
(
    `id`           bigint(20)                       NOT NULL COMMENT 'id',
    `portfolio_id` bigint(20)                       NOT NULL COMMENT '项目集ID',
    `company_id`   bigint(20)                       NOT NULL COMMENT '公司ID',
    `project_id`   bigint(20)                       NOT NULL COMMENT '项目ID',
    `create_user`  varchar(128) COLLATE utf8mb4_bin NOT NULL COMMENT '创建者',
    `create_time`  datetime                         NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `company_portfolio_id` (`company_id`, `portfolio_id`, `project_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='项目集关联项目';

create table portfolio_chart
(
    `id`               bigint                  not null comment '报表id',
    `portfolio_id`     bigint(20)              NOT NULL COMMENT '项目集ID',
    `title`            varchar(32) default '0' not null comment '标题',
    `chart_type`       varchar(20)             not null comment 'chart类型',
    `create_time`      datetime                not null comment '创建时间',
    `create_user`      varchar(128)            not null comment '创建人',
    `last_modify_time` datetime                not null comment '最近修改时间',
    `last_modify_user` varchar(128)            not null comment '最近修改人',
    `seq_num`          int(10)     default 0   not null comment '顺序编号',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin
    comment '报表';

create table portfolio_favourite
(
    `id`           bigint(20)   not null comment 'ID',
    `create_user`  varchar(128) not null comment '创建人',
    `create_time`  datetime     not null comment '收藏时间',
    `company_id`   bigint(20)   not null comment '公司',
    `portfolio_id` bigint(20)   not null comment '项目ID',
    primary key (`id`),
    KEY `user` (`create_user`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin
    comment '项目集收藏';

CREATE TABLE `card_alarm_notice_plan`
(
    `id`               bigint(20)        NOT NULL COMMENT 'id',
    `project_id`       bigint(20)        NOT NULL COMMENT '项目ID',
    `card_id`          bigint(20)        NOT NULL COMMENT '卡片ID',
    `alarm_id`         bigint(20)        NOT NULL COMMENT '触发的预警配置ID',
    `timestamp_minute` int(11)           NOT NULL COMMENT '预警触发通知的时间戳（精确到分）',
    `send_flag`        int(11) default 0 NOT NULL COMMENT '发送标记,0-未发送，1-已发送',
    PRIMARY KEY (`id`),
    KEY `card_id` (`card_id`) USING BTREE,
    KEY `alarm_id` (`alarm_id`) USING BTREE,
    KEY `timestamp_minute` (`timestamp_minute`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='卡片通知计划';

create table `project_alarm`
(
    `id`         bigint           not null comment 'id'
        primary key,
    `project_id` bigint default 0 not null comment '项目ID',
    `name`       varchar(32)      not null comment '规则名称'
)
    comment '项目中的预警规则';

SET FOREIGN_KEY_CHECKS = 1;
