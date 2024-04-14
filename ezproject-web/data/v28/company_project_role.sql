ALTER TABLE `project_member`
  ADD COLUMN `role_source` varchar(40) COLLATE utf8mb4_bin NOT NULL DEFAULT 'CUSTOM' COMMENT '角色来源',
  ADD COLUMN `role_type` varchar(40) COLLATE utf8mb4_bin NOT NULL DEFAULT 'MEMBER' COMMENT '角色类型';

UPDATE `project_member` SET `role_source` = 'SYS', `role_type` = 'ADMIN' WHERE `role` = 'ADMIN';
UPDATE `project_member` SET `role_source` = 'SYS', `role_type` = 'MEMBER' WHERE `role` = 'MEMBER';
UPDATE `project_member` SET `role_source` = 'SYS', `role_type` = 'GUEST' WHERE `role` = 'GUEST';


UPDATE `project_member` SET `role_source` = 'CUSTOM', `role_type` = 'MEMBER' WHERE `role_source` != 'COMPANY' AND `role` NOT IN('ADMIN', 'MEMBER', 'GUEST');

