ALTER TABLE `project_repo`
    ADD COLUMN `repo_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '代码库ID';

ALTER TABLE `project_repo`
    ADD KEY `repo_id` (`repo_id`) USING BTREE;