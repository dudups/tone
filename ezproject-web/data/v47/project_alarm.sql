CREATE TABLE `card_alarm_notice_plan`
(
    `id`               bigint(20)        NOT NULL COMMENT 'id',
    `project_id`       bigint(20)        NOT NULL COMMENT '项目ID',
    `card_id`          bigint(20)        NOT NULL COMMENT '卡片ID',
    `alarm_id`         bigint(20)        NOT NULL COMMENT '触发的预警配置ID',
    `timestamp_minute` int(11)           NOT NULL COMMENT '预警触发通知的时间戳（精确到分）',
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

alter table card_alarm_notice_plan
    add send_flag int default 0 null comment '发送标记：0-未发送，1-已发送';