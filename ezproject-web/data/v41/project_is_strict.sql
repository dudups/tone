alter table project
    add `is_strict` bit(1) NOT NULL DEFAULT b'0' COMMENT '严格模式';