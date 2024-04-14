ALTER TABLE story_map MODIFY COLUMN `name` varchar(32) COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE story_map_node MODIFY COLUMN `name` varchar(32) COLLATE utf8mb4_bin NOT NULL;
