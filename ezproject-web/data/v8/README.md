背景：自动添加的文本字段索引类型为text，实际上需要是keyword; 暴露出的问题是自定义报表统计，因为是text导致无法分类；
解决：
1. reindex这两张索引表project-card和project-card-event; 其它索引表有类似问题，但是没有影响，暂时不用管；
2. 对于增量更新的环境，后续牵涉变更字段，尤其是字符传类型字段，通过准备运维脚本提前设置索引表；