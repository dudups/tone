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
    UNIQUE KEY `company_portfolio_id` (`company_id`, `portfolio_id`, `project_id`),
    KEY `project_id` (`project_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='项目集关联项目';

create table portfolio_chart
(
    `id`               bigint(20)              not null comment '报表id',
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



