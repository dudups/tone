## 私有部署的用户，归档计划保存天数，将默认值为365的修改成不清除归档计划。
update project
set project.plan_keep_days = 0
where project.plan_keep_days = 365;
